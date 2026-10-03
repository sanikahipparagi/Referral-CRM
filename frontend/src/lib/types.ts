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
  followUpDays: number;
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

export type AssistantContact = {
  id: string; name: string; designation: string | null; companyId: string | null; companyName: string | null;
  location: string | null; linkedinUrl: string | null; status: ContactStatus; recommendationScore: number;
  reasoning: string; needsFollowUp: boolean;
};
export type OpportunityQueue = {
  contacts: AssistantContact[];
  suggestedContacts: AssistantContact[];
  suggestedCompanies: Array<{ id: string; name: string; priority: number; dreamCompany: boolean; applicationStatus: string }>;
};
export type Resume = { id: string; label: string; fileName: string };
export type GeneratedMessage = {
  id: string; contactId: string; contactName: string; linkedinUrl: string | null; companyId: string | null;
  companyName: string | null; resumeId: string | null; resumeLabel: string | null; resumeReason: string | null;
  role: string | null; variant: string; version: number; channel: string; messageText: string;
  recommendationScore: number; recommendationReason: string | null; status: "DRAFT" | "APPROVED" | "SENT";
  createdAt: string;
};
export type PromptTemplate = { id: string; category: string; version: number; promptText: string };
export type RecommendationRule = { id?: string; category: string; keyword: string; weight: number; enabled: boolean };
export type MetricGroup = { name: string; count: number; replies: number; responseRate: number };
export type NetworkingAnalytics = {
  responseRate: number; referralRate: number; interviewRate: number; offerRate: number; averageReplyHours: number;
  topCompanies: MetricGroup[]; topRoles: MetricGroup[]; mostSuccessfulResume: string | null;
  bestPerformingMessage: string | null; mostResponsiveContactType: string | null;
};
