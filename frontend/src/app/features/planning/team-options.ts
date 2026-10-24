import { PlanningTeam } from './services/planning.service';

export interface TeamOption {
  id: string;
  label: string;
}

/**
 * EF-017 : options du filtre d'équipe, chaque sous-équipe rangée sous son équipe parente avec un
 * retrait par niveau. Une sous-équipe dont le parent est hors périmètre devient une racine.
 */
export function toTeamOptions(teams: PlanningTeam[]): TeamOption[] {
  const visibleIds = new Set(teams.map((team) => team.id));
  const parentOf = (team: PlanningTeam) =>
    team.parentTeamId && visibleIds.has(team.parentTeamId) ? team.parentTeamId : null;
  const childrenOf = (parentId: string | null) =>
    teams
      .filter((team) => parentOf(team) === parentId)
      .sort((a, b) => a.name.localeCompare(b.name));

  const options: TeamOption[] = [];
  const visit = (team: PlanningTeam, depth: number) => {
    options.push({ id: team.id, label: `${'— '.repeat(depth)}${team.name}` });
    childrenOf(team.id).forEach((child) => visit(child, depth + 1));
  };
  childrenOf(null).forEach((root) => visit(root, 0));
  return options;
}
