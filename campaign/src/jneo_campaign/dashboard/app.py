from __future__ import annotations

from fastapi import FastAPI, HTTPException, Request
from fastapi.responses import HTMLResponse
from fastapi.templating import Jinja2Templates
from pydantic import BaseModel
from sqlalchemy import func, select

from jneo_campaign.config import CAMPAIGN_ROOT, Settings
from jneo_campaign.storage.database import Database
from jneo_campaign.storage.models import (
    AuditEvent,
    Campaign,
    ComplianceDecision,
    DemoPlan,
    Domain,
    EmailMessage,
    EmailThread,
    GeneratedAsset,
    Meeting,
    Organization,
    PauseControl,
    ProspectScore,
    ProviderFailure,
    ReplyClassification,
    SuppressionEntry,
)


class PauseRequest(BaseModel):
    paused: bool = True
    reason: str = "Paused from local dashboard"


def create_app(settings: Settings | None = None) -> FastAPI:
    settings = settings or Settings()
    database = Database(settings.campaign_database_url)
    database.create_all()
    templates = Jinja2Templates(directory=CAMPAIGN_ROOT / "templates")
    app = FastAPI(title="Jneopallium Campaign Dashboard", version="0.1.0")

    @app.get("/", response_class=HTMLResponse)
    def dashboard(request: Request):
        with database.session() as session:
            context = {
                "request": request,
                "mode": settings.campaign_mode,
                "live_enabled": settings.live_writes_enabled,
                "counts": {
                    "organizations": session.scalar(select(func.count(Organization.id))) or 0,
                    "queued": session.scalar(
                        select(func.count(EmailMessage.id)).where(EmailMessage.status == "QUEUED")
                    )
                    or 0,
                    "replies": session.scalar(select(func.count(ReplyClassification.id))) or 0,
                    "meetings": session.scalar(
                        select(func.count(Meeting.id)).where(Meeting.status == "MEETING_SCHEDULED")
                    )
                    or 0,
                    "blocks": session.scalar(
                        select(func.count(ComplianceDecision.id)).where(
                            ComplianceDecision.decision.not_in(["APPROVED", "APPROVED_DRY_RUN"])
                        )
                    )
                    or 0,
                    "provider_failures": session.scalar(
                        select(
                            func.count(ProviderFailure.id).where(
                                ProviderFailure.resolved.is_(False)
                            )
                        )
                    )
                    or 0,
                },
                "domains": list(
                    session.scalars(select(Domain).order_by(Domain.score.desc()).limit(15))
                ),
                "prospects": list(
                    session.execute(
                        select(Organization, ProspectScore)
                        .join(ProspectScore, ProspectScore.organization_id == Organization.id)
                        .order_by(ProspectScore.total.desc())
                        .limit(20)
                    )
                ),
                "campaigns": list(session.scalars(select(Campaign))),
                "threads": list(
                    session.scalars(
                        select(EmailThread).order_by(EmailThread.updated_at.desc()).limit(20)
                    )
                ),
                "meetings_list": list(
                    session.scalars(select(Meeting).order_by(Meeting.updated_at.desc()).limit(20))
                ),
                "suppressions": list(
                    session.scalars(
                        select(SuppressionEntry).where(SuppressionEntry.active.is_(True)).limit(20)
                    )
                ),
                "assets": list(
                    session.scalars(
                        select(GeneratedAsset).order_by(GeneratedAsset.updated_at.desc()).limit(20)
                    )
                ),
                "demo_plans": list(
                    session.scalars(select(DemoPlan).order_by(DemoPlan.updated_at.desc()).limit(20))
                ),
                "audit": list(
                    session.scalars(
                        select(AuditEvent).order_by(AuditEvent.occurred_at.desc()).limit(25)
                    )
                ),
            }
        return templates.TemplateResponse("dashboard.html", context)

    @app.get("/api/health")
    def health() -> dict[str, object]:
        with database.session() as session:
            campaign = session.scalar(select(Campaign).limit(1))
            failures = (
                session.scalar(
                    select(func.count(ProviderFailure.id)).where(
                        ProviderFailure.resolved.is_(False)
                    )
                )
                or 0
            )
        return {
            "status": "paused" if campaign and campaign.paused else "ok",
            "mode": settings.campaign_mode,
            "live_writes_enabled": settings.live_writes_enabled,
            "unresolved_provider_failures": failures,
        }

    @app.post("/api/pause/{scope}/{value}")
    def pause(scope: str, value: str, payload: PauseRequest) -> dict[str, object]:
        scope = scope.upper()
        with database.session() as session:
            if scope == "CAMPAIGN":
                item = session.scalar(select(Campaign).where(Campaign.campaign_key == value))
                if item is None:
                    raise HTTPException(404, "Campaign not found")
                item.paused = payload.paused
            elif scope == "DOMAIN":
                item = session.scalar(select(Domain).where(Domain.slug == value))
                if item is None:
                    raise HTTPException(404, "Domain not found")
                item.paused = payload.paused
            elif scope == "ORGANIZATION":
                item = session.get(Organization, int(value))
                if item is None:
                    raise HTTPException(404, "Organization not found")
                item.paused = payload.paused
            elif scope == "SEQUENCE":
                item = session.get(EmailThread, int(value))
                if item is None:
                    raise HTTPException(404, "Email sequence not found")
                item.sequence_paused = payload.paused
            elif scope == "REGION":
                item = session.scalar(
                    select(PauseControl).where(
                        PauseControl.scope == "REGION", PauseControl.value == value
                    )
                )
                if item is None:
                    item = PauseControl(scope="REGION", value=value)
                    session.add(item)
                item.paused = payload.paused
                item.reason = payload.reason
            else:
                raise HTTPException(
                    400, "Scope must be campaign, domain, region, organization, or sequence"
                )
        return {"scope": scope, "value": value, "paused": payload.paused}

    return app


app = create_app()
