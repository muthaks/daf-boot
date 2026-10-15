-- CASDM-1352: roster roles allow more than one person per role. The captured value is a
-- `;`-joined list of display names, so the 255-char role columns are widened.

ALTER TABLE [dbo].[AGI_AgendaItem] ALTER COLUMN [Chair]             nvarchar(1000) NULL
GO
ALTER TABLE [dbo].[AGI_AgendaItem] ALTER COLUMN [LecRep]            nvarchar(1000) NULL
GO
ALTER TABLE [dbo].[AGI_AgendaItem] ALTER COLUMN [EqMemberFrmExec]   nvarchar(1000) NULL
GO
ALTER TABLE [dbo].[AGI_AgendaItem] ALTER COLUMN [BusinessRep1]      nvarchar(1000) NULL
GO
ALTER TABLE [dbo].[AGI_AgendaItem] ALTER COLUMN [SeniorBusinessRep] nvarchar(1000) NULL
GO
ALTER TABLE [dbo].[AGI_AgendaItem] ALTER COLUMN [PortfolioCce1]     nvarchar(1000) NULL
GO
ALTER TABLE [dbo].[AGI_AgendaItem] ALTER COLUMN [PortfolioCce2]     nvarchar(1000) NULL
GO
ALTER TABLE [dbo].[AGI_AgendaItem] ALTER COLUMN [IndependentMember] nvarchar(1000) NULL
GO
ALTER TABLE [dbo].[AGI_AgendaItem] ALTER COLUMN [Scribe]            nvarchar(1000) NULL
GO

-- Audit table mirrors the same widths.
ALTER TABLE [dbo].[A_AGI_AgendaItem] ALTER COLUMN [Chair]             nvarchar(1000) NULL
GO
ALTER TABLE [dbo].[A_AGI_AgendaItem] ALTER COLUMN [LecRep]            nvarchar(1000) NULL
GO
ALTER TABLE [dbo].[A_AGI_AgendaItem] ALTER COLUMN [EqMemberFrmExec]   nvarchar(1000) NULL
GO
ALTER TABLE [dbo].[A_AGI_AgendaItem] ALTER COLUMN [BusinessRep1]      nvarchar(1000) NULL
GO
ALTER TABLE [dbo].[A_AGI_AgendaItem] ALTER COLUMN [SeniorBusinessRep] nvarchar(1000) NULL
GO
ALTER TABLE [dbo].[A_AGI_AgendaItem] ALTER COLUMN [PortfolioCce1]     nvarchar(1000) NULL
GO
ALTER TABLE [dbo].[A_AGI_AgendaItem] ALTER COLUMN [PortfolioCce2]     nvarchar(1000) NULL
GO
ALTER TABLE [dbo].[A_AGI_AgendaItem] ALTER COLUMN [IndependentMember] nvarchar(1000) NULL
GO
ALTER TABLE [dbo].[A_AGI_AgendaItem] ALTER COLUMN [Scribe]            nvarchar(1000) NULL
GO
