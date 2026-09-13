export type InventoryLocation = 'CENTRAL' | 'MASTER' | 'EXECUTIVE' | 'ASSIGNED';

export type ProofType = 
  | 'EXISTING_STOCK'
  | 'CENTRAL_RECEIVED'
  | 'COURIER_TO_EXECUTIVE'
  | 'TAG_ASSIGNED'
  | 'USER_TAG_UPLOAD';

export type UserRole = 'ADMIN' | 'USER';

export interface LoggedInUser {
  role: UserRole;
  userId: string;
  displayName: string;
  region: string;
}

export interface InventoryTagEntity {
  tagSerialNumber: string;
  tagClass: string;
  currentLocation: InventoryLocation;
  assignedExecutiveId?: string | null;
  vehicleNumber?: string | null;
  lastProofDocumentId: number;
  updatedAt: number;
}

export interface ExecutiveEntity {
  executiveId: string;
  executiveName: string;
  region: string;
  active: boolean;
}

export interface ProofDocumentEntity {
  id: number;
  proofType: ProofType;
  originalFilename: string;
  storageObjectKey: string;
  uploadedAt: number;
  uploadedBy: string;
  rawExtractedText?: string | null;
  fileSize: number;
  previewUrl?: string | null;
}

export interface StockMovementLedgerEntity {
  id: number;
  tagSerialNumber: string;
  tagClass: string;
  previousLocation?: InventoryLocation | null;
  newLocation: InventoryLocation;
  subagentId: string;
  subagentName: string;
  proofDocumentId: number;
  createdAt: number;
}

export interface OverallStockMetrics {
  centralStock: number;
  masterStock: number;
  executiveStock: number;
  assignedStock: number;
  totalActive: number;
}

export interface ClassStockSummary {
  tagClass: string;
  centralCount: number;
  masterCount: number;
  executiveCount: number;
  assignedCount: number;
  totalCount: number;
}

export interface ExecutiveStockSummary {
  executiveId: string;
  executiveName: string;
  region: string;
  active: boolean;
  currentStock: number;
}

export interface MovementWithProof {
  id: number;
  tagSerialNumber: string;
  tagClass: string;
  previousLocation?: InventoryLocation | null;
  newLocation: InventoryLocation;
  subagentId: string;
  subagentName: string;
  proofDocumentId: number;
  createdAt: number;
  originalFilename?: string;
  proofType?: string;
  uploadedAt?: number;
}

export interface ExtractedTagCandidate {
  id: string;
  subagentId: string;
  subagentName: string;
  tagClass: string;
  isRange: boolean;
  serialNumber: string;
  fromSerial: string;
  toSerial: string;
  calculatedQuantity: number;
}

export interface TransactionValidationResult {
  isValid: boolean;
  totalCount: number;
  classBreakdown: Record<string, number>;
  candidateSerials: string[];
  duplicateCount: number;
  invalidTransitionCount: number;
  errorMessage?: string | null;
}

export interface TransactionExecutionResult {
  success: boolean;
  proofId: number;
  affectedCount: number;
  message: string;
}

export type AppScreen = 'DASHBOARD' | 'WORKFLOW' | 'EXECUTIVES' | 'SEARCH' | 'PROOFS' | 'REPORTS' | 'USER_UPLOAD';

export type WorkflowStep = 'SELECT_DOCUMENT' | 'OCR_PROCESSING' | 'VERIFICATION' | 'PREVIEW_SUMMARY' | 'SUCCESS_RESULT';
