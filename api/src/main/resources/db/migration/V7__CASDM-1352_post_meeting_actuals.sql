-- CASDM-1352: post-meeting performance-monitoring actuals (Agenda_Builder_Spec.md section 9).
-- Captures the actual rating, purpose, DCP and committee date alongside the existing
-- actual pages / time / decision / conditions.

ALTER TABLE [dbo].[AGPM_AgendaPostMeeting] ADD
    [ActualRating]   nvarchar(100) NULL,
    [ActualPurpose]  nvarchar(255) NULL,
    [ActualDcp]      float         NULL,
    [DateOfCc]       datetime      NULL
GO

ALTER TABLE [dbo].[A_AGPM_AgendaPostMeeting] ADD
    [ActualRating]   nvarchar(100) NULL,
    [ActualPurpose]  nvarchar(255) NULL,
    [ActualDcp]      float         NULL,
    [DateOfCc]       datetime      NULL
GO
