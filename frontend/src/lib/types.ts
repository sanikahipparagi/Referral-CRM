export type User = { id: string; email: string; fullName: string };

export const contactStatuses = [
  "NOT_CONTACTED",
  "MESSAGE_READY",
  "CONTACTED",
  "REPLIED",
  "REFERRED",
  "INTERVIEW",
  "REJECTED",
  "NO_RESPONSE",
] as const;
export type ContactStatus = (typeof contactStatuses)[number];

export type Contact = {
  id: string;
  name: string;
  companyId: string | null;
  companyName?: string | null;
  linkedinUrl: string | null;
  designation: string | null;
  location: string | null;
  email: string | null;
  source: string | null;
  dateAdded: string;
  notes: string | null;
  status: ContactStatus;
  createdAt: string;
};

export type Company = {
  id: string;
  name: string;
  careerPage: string | null;
  priority: number;
  dreamCompany: boolean;
  applicationStatus: string;
  notes: string | null;
  createdAt: string;
};

export type Page<T> = {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
};

export type DashboardData = {
  date: string;
  todayOutreach: number;
  peopleContacted: number;
  pendingFollowUps: number;
  replies: number;
  referralsReceived: number;
  interviews: number;
  responseRate: number;
  referralRate: number;
  followUps: Array<{ id: string; name: string; designation: string | null; companyName: string | null; lastSentAt: string }>;
  companies: Array<{ id: string; name: string; contactCount: number; outreachCount: number; referralCount: number }>;
};
