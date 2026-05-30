export type ProfileCategory =
  | 'DEVELOPER'
  | 'BACKEND_DEVELOPER'
  | 'FRONTEND_DEVELOPER'
  | 'FULLSTACK_DEVELOPER'
  | 'DEVOPS'
  | 'DATA_ML'
  | 'BLOCKCHAIN_WEB3'
  | 'DESIGNER'
  | 'BUSINESS_FOUNDER'
  | 'AGENCY_STUDIO';

export interface PublicMerchantSummary {
  id:          number;
  name:        string;
  slug:        string;
  description: string;
  avatarUrl:   string;
}

export interface PublicProfile {
  id:               number;
  slug:             string;
  displayName:      string;
  headline:         string;
  bio:              string;
  location:         string;
  websiteUrl:       string | null;
  category:         ProfileCategory;
  tags:             string[];
  githubUsername:   string;
  githubAvatarUrl:  string;
  githubProfileUrl: string;
  githubPublicRepos:number;
  githubFollowers:  number;
  isPublic:         boolean;
  merchants:        PublicMerchantSummary[];
}

export const CATEGORY_LABELS: Record<ProfileCategory, string> = {
  DEVELOPER:          'Developer',
  BACKEND_DEVELOPER:  'Backend',
  FRONTEND_DEVELOPER: 'Frontend',
  FULLSTACK_DEVELOPER:'Full Stack',
  DEVOPS:             'DevOps',
  DATA_ML:            'Data / ML',
  BLOCKCHAIN_WEB3:    'Web3',
  DESIGNER:           'Designer',
  BUSINESS_FOUNDER:   'Founder',
  AGENCY_STUDIO:      'Agency',
};
