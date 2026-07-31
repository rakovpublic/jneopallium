from __future__ import annotations

import hashlib

from sqlalchemy import select
from sqlalchemy.orm import Session

from jneo_campaign.storage.models import Campaign, Experiment, Organization


class ExperimentService:
    def ensure_default(self, session: Session, campaign_key: str) -> Experiment | None:
        campaign = session.scalar(select(Campaign).where(Campaign.campaign_key == campaign_key))
        if campaign is None:
            return None
        item = session.scalar(
            select(Experiment).where(
                Experiment.campaign_id == campaign.id,
                Experiment.hypothesis
                == "A demo-first message yields more legitimate technical replies",
            )
        )
        if item is None:
            item = Experiment(
                campaign_id=campaign.id,
                hypothesis="A demo-first message yields more legitimate technical replies",
                variables=["framing"],
                variants=[
                    {"key": "demo_first", "framing": "lead with relevant deterministic demo"},
                    {"key": "conversation_first", "framing": "lead with problem-fit question"},
                ],
                stopping_rule="Evaluate after 30 delivered messages per variant; stop early only for safety/compliance",
                minimum_sample_size=60,
                status="ACTIVE",
            )
            session.add(item)
            session.flush()
        assignments = dict(item.assignment or {})
        for organization in session.scalars(select(Organization).order_by(Organization.id)):
            key = str(organization.id)
            assignments.setdefault(
                key,
                "demo_first"
                if int(hashlib.sha256(key.encode()).hexdigest(), 16) % 2 == 0
                else "conversation_first",
            )
        item.assignment = assignments
        # Result selection is intentionally absent until the minimum sample and stopping rule pass.
        return item
