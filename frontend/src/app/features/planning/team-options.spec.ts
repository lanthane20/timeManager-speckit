import { toTeamOptions } from './team-options';

describe('toTeamOptions', () => {
  it('range chaque sous-équipe sous son équipe parente, avec un retrait par niveau', () => {
    const options = toTeamOptions([
      { id: 'commercial', name: 'Commercial', parentTeamId: null },
      { id: 'nuit', name: 'Atelier nuit', parentTeamId: 'atelier' },
      { id: 'production', name: 'Production', parentTeamId: null },
      { id: 'atelier', name: 'Atelier', parentTeamId: 'production' },
    ]);

    expect(options).toEqual([
      { id: 'commercial', label: 'Commercial' },
      { id: 'production', label: 'Production' },
      { id: 'atelier', label: '— Atelier' },
      { id: 'nuit', label: '— — Atelier nuit' },
    ]);
  });

  it('traite comme racine une sous-équipe dont le parent est hors périmètre', () => {
    const options = toTeamOptions([{ id: 'atelier', name: 'Atelier', parentTeamId: 'production' }]);

    expect(options).toEqual([{ id: 'atelier', label: 'Atelier' }]);
  });
});
