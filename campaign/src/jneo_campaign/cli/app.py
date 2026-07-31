from __future__ import annotations

import json
import logging
import platform
import sys
from typing import Any

import typer
from pythonjsonlogger.json import JsonFormatter

from jneo_campaign.config import load_config
from jneo_campaign.orchestrator import CampaignOrchestrator
from jneo_campaign.providers.credentials import EncryptedCredentialStore
from jneo_campaign.providers.google import GOOGLE_SCOPES, GoogleCredentialManager

app = typer.Typer(no_args_is_help=True, help="Jneopallium campaign automation")
auth_app = typer.Typer(help="Google OAuth operations")
inventory_app = typer.Typer(help="Repository capability inventory")
domains_app = typer.Typer(help="Domain research")
prospects_app = typer.Typer(help="Prospect discovery and verification")
offers_app = typer.Typer(help="Offer generation")
demos_app = typer.Typer(help="Demo matching")
outreach_app = typer.Typer(help="Outreach preparation and sending")
inbox_app = typer.Typer(help="Campaign-only Gmail synchronization")
followups_app = typer.Typer(help="Follow-up operations")
meetings_app = typer.Typer(help="Meeting operations")
analytics_app = typer.Typer(help="Campaign analytics")

app.add_typer(auth_app, name="auth")
app.add_typer(inventory_app, name="inventory")
app.add_typer(domains_app, name="domains")
app.add_typer(prospects_app, name="prospects")
app.add_typer(offers_app, name="offers")
app.add_typer(demos_app, name="demos")
app.add_typer(outreach_app, name="outreach")
app.add_typer(inbox_app, name="inbox")
app.add_typer(followups_app, name="followups")
app.add_typer(meetings_app, name="meetings")
app.add_typer(analytics_app, name="analytics")


def _configure_logging(level: str) -> None:
    handler = logging.StreamHandler()
    handler.setFormatter(JsonFormatter("%(asctime)s %(levelname)s %(name)s %(message)s"))
    root = logging.getLogger()
    root.handlers[:] = [handler]
    root.setLevel(level.upper())


def _orchestrator() -> CampaignOrchestrator:
    config = load_config()
    _configure_logging(config.settings.campaign_log_level)
    return CampaignOrchestrator(config=config)


def _print(value: Any) -> None:
    typer.echo(json.dumps(value, indent=2, ensure_ascii=False, default=str))


@app.command()
def doctor(validate_credentials: bool = typer.Option(False, "--validate-credentials")) -> None:
    """Validate configuration, repository, database, and optionally Google credentials."""
    runner = _orchestrator()
    settings = runner.config.settings
    checks: dict[str, Any] = {
        "python": {"version": platform.python_version(), "ok": sys.version_info >= (3, 12)},
        "repository": {
            "path": str(settings.campaign_repository_root.resolve()),
            "ok": (settings.campaign_repository_root / "pom.xml").exists(),
        },
        "configuration": {
            "config_dir": str(settings.campaign_config_dir.resolve()),
            "ok": settings.campaign_config_dir.exists(),
        },
        "database": {"url_scheme": settings.campaign_database_url.split(":", 1)[0], "ok": True},
        "safety": {
            "mode": settings.campaign_mode,
            "live_send_flag": settings.campaign_live_send,
            "external_writes_enabled": settings.live_writes_enabled,
            "ok": not settings.campaign_live_send or settings.campaign_mode == "LIVE",
        },
        "credentials": {"checked": False, "ok": not settings.live_writes_enabled},
    }
    if validate_credentials or settings.live_writes_enabled:
        try:
            checks["credentials"] = {
                "checked": True,
                "gmail": runner.providers.gmail.validate_credentials(),
                "calendar": runner.providers.calendar.validate_credentials(),
                "ok": True,
            }
        except Exception as exc:
            checks["credentials"] = {
                "checked": True,
                "ok": False,
                "error": f"{type(exc).__name__}: {exc}",
            }
    checks["ok"] = all(
        value.get("ok", True) for value in checks.values() if isinstance(value, dict)
    )
    _print(checks)
    if not checks["ok"]:
        raise typer.Exit(1)


def _authorize_google() -> None:
    config = load_config()
    settings = config.settings
    if not settings.google_oauth_client_file.exists():
        raise typer.BadParameter(
            f"OAuth client file not found: {settings.google_oauth_client_file}"
        )
    manager = GoogleCredentialManager(
        str(settings.google_oauth_client_file),
        EncryptedCredentialStore(settings.google_oauth_token_file, settings.google_oauth_account),
    )
    credentials = manager.authorize(GOOGLE_SCOPES)
    _print({"authorized": True, "scopes": credentials.scopes, "token_encrypted": True})


@auth_app.command("gmail")
def auth_gmail() -> None:
    """Authorize the minimum Gmail and Calendar scopes required by the full campaign."""
    _authorize_google()


@auth_app.command("calendar")
def auth_calendar() -> None:
    """Authorize the minimum Gmail and Calendar scopes required by the full campaign."""
    _authorize_google()


@auth_app.command("validate")
def auth_validate() -> None:
    doctor(validate_credentials=True)


@inventory_app.command("build")
def inventory_build() -> None:
    runner = _orchestrator()
    with runner.database.session() as session:
        _print(runner._inventory(session))


@domains_app.command("research")
def domains_research() -> None:
    runner = _orchestrator()
    with runner.database.session() as session:
        _print(runner._domain_research(session))


@prospects_app.command("discover")
def prospects_discover() -> None:
    runner = _orchestrator()
    with runner.database.session() as session:
        _print(runner._prospect_discovery(session))


@prospects_app.command("verify")
def prospects_verify() -> None:
    runner = _orchestrator()
    with runner.database.session() as session:
        _print(runner.verification.verify(session))


@offers_app.command("generate")
def offers_generate() -> None:
    runner = _orchestrator()
    with runner.database.session() as session:
        scores = runner.scoring.score(session)
        _print({"scored": len(scores), "offers": len(runner.offers.generate(session))})


@demos_app.command("match")
def demos_match() -> None:
    runner = _orchestrator()
    with runner.database.session() as session:
        _print({"plans": len(runner.demos.match(session))})


@outreach_app.command("prepare")
def outreach_prepare() -> None:
    runner = _orchestrator()
    with runner.database.session() as session:
        runner.assets.generate(session, limit=5)
        runner.compliance.review(session)
        _print({"prepared": len(runner.outreach.prepare(session))})


@outreach_app.command("send")
def outreach_send() -> None:
    runner = _orchestrator()
    with runner.database.session() as session:
        _print({"sent_or_simulated": len(runner.outreach.send(session))})


@inbox_app.command("sync")
def inbox_sync() -> None:
    runner = _orchestrator()
    with runner.database.session() as session:
        _print({"classified": len(runner.replies.sync(session))})


@followups_app.command("run")
def followups_run() -> None:
    runner = _orchestrator()
    with runner.database.session() as session:
        runner.followups.schedule(session)
        _print({"sent": len(runner.followups.run(session))})


@meetings_app.command("sync")
def meetings_sync() -> None:
    runner = _orchestrator()
    with runner.database.session() as session:
        proposed = runner.meetings.propose(session)
        scheduled = runner.meetings.confirm_agreements(session)
        _print({"proposed": len(proposed), "scheduled": len(scheduled)})


@analytics_app.command("report")
def analytics_report() -> None:
    runner = _orchestrator()
    with runner.database.session() as session:
        _print(runner.analytics.report(session, runner.config.campaign.campaign_id))


@app.command("run")
def run(
    once: bool = typer.Option(False, "--once", help="Run one complete state-machine cycle"),
    continuous: bool = typer.Option(False, "--continuous", help="Run continuously"),
    interval_minutes: int = typer.Option(60, min=5),
) -> None:
    """Run the complete campaign workflow."""
    if once == continuous:
        raise typer.BadParameter("Choose exactly one of --once or --continuous")
    runner = _orchestrator()
    if once:
        _print(runner.run_once())
    else:
        runner.run_continuous(interval_minutes=interval_minutes)


@app.command("dashboard")
def dashboard(
    host: str = typer.Option("127.0.0.1"),
    port: int = typer.Option(8765, min=1, max=65535),
) -> None:
    """Serve the local dashboard."""
    import uvicorn

    uvicorn.run("jneo_campaign.dashboard.app:app", host=host, port=port, reload=False)


def main() -> None:
    app()


if __name__ == "__main__":
    main()
