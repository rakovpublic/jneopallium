"""Initial campaign schema.

Revision ID: 0001
Revises: None
"""
from jneo_campaign.storage.models import Base

revision = "0001"
down_revision = None
branch_labels = None
depends_on = None


def upgrade() -> None:
    from alembic import op

    Base.metadata.create_all(bind=op.get_bind())


def downgrade() -> None:
    from alembic import op

    Base.metadata.drop_all(bind=op.get_bind())
