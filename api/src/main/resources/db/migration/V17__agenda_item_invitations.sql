-- One calendar invitation per approved agenda item (BR-013): only the item's own participants are
-- invited, for the item's slot, with the item's paper. The meeting keeps its own event for the
-- committee's standing roles (chair, alternate chair, secretary, minute taker, administrator).
ALTER TABLE [dbo].[AGI_AgendaItem] ADD
    [GraphEventId]       nvarchar(255) NULL,
    [TeamsJoinUrl]       nvarchar(1000) NULL,
    [InvitationSnapshot] nvarchar(64)  NULL,
    [InvitationSentOn]   datetime      NULL,
    [InvitationSentBy]   nvarchar(255) NULL
GO
ALTER TABLE [dbo].[A_AGI_AgendaItem] ADD
    [GraphEventId]       nvarchar(255) NULL,
    [TeamsJoinUrl]       nvarchar(1000) NULL,
    [InvitationSnapshot] nvarchar(64)  NULL,
    [InvitationSentOn]   datetime      NULL,
    [InvitationSentBy]   nvarchar(255) NULL
GO
