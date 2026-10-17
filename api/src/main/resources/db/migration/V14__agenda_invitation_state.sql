-- Keep invitation delivery separate from the agenda editing lifecycle.
ALTER TABLE [dbo].[AGM_AgendaMeeting] ADD
    [InvitationSnapshot] nvarchar(64) NULL,
    [InvitationSentOn] datetime NULL,
    [InvitationSentBy] nvarchar(255) NULL,
    [InvitationReopened] bit NOT NULL CONSTRAINT [DF_AGM_InvitationReopened] DEFAULT 0
GO
ALTER TABLE [dbo].[A_AGM_AgendaMeeting] ADD
    [InvitationSnapshot] nvarchar(64) NULL,
    [InvitationSentOn] datetime NULL,
    [InvitationSentBy] nvarchar(255) NULL,
    [InvitationReopened] bit NULL
GO
