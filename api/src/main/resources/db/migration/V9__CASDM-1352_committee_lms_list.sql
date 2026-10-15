-- CASDM-1352: link an agenda committee (per mandate level) to a List Management list,
-- so committee membership can be sourced from LMS instead of free-typed. Nullable so
-- existing committees keep their free-text members until a list is assigned.

ALTER TABLE [dbo].[AGC_AgendaCommittee] ADD
    [ListName] nvarchar(255) NULL
GO

ALTER TABLE [dbo].[A_AGC_AgendaCommittee] ADD
    [ListName] nvarchar(255) NULL
GO
