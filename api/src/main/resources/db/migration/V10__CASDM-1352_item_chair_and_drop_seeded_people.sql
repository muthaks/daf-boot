-- CASDM-1352: the chair is decided per agenda item (it varies within a meeting - see the
-- workbook "Agenda output for editing" examples), and people are never predefined on the
-- committee. Add a per-item Chair, and clear the seeded committee chair initials.

ALTER TABLE [dbo].[AGI_AgendaItem] ADD
    [Chair] nvarchar(255) NULL
GO

ALTER TABLE [dbo].[A_AGI_AgendaItem] ADD
    [Chair] nvarchar(255) NULL
GO

-- Committee people are resolved from the RMB name directory / LMS at item time, not seeded.
UPDATE [dbo].[AGC_AgendaCommittee]
   SET [Chair] = NULL, [ChairId] = NULL
GO
