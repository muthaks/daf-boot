-- CASDM-1352: post-meeting lock. Once the chair signs off the post-meeting record it
-- is locked from further edits; only a reviewer may unlock it (Agenda_Builder_Spec.md
-- section 9). Post-meeting capture itself is gated to meetings that have been marked Held.

ALTER TABLE [dbo].[AGPM_AgendaPostMeeting] ADD
    [Locked]   bit           NOT NULL DEFAULT ((0)),
    [LockedBy] nvarchar(255) NULL,
    [LockedOn] datetime      NULL
GO

ALTER TABLE [dbo].[A_AGPM_AgendaPostMeeting] ADD
    [Locked]   bit           NULL,
    [LockedBy] nvarchar(255) NULL,
    [LockedOn] datetime      NULL
GO
