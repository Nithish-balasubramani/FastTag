import {
  InventoryLocation,
  ProofType,
  InventoryTagEntity,
  ExecutiveEntity,
  ProofDocumentEntity,
  StockMovementLedgerEntity,
  OverallStockMetrics,
  ClassStockSummary,
  ExecutiveStockSummary,
  MovementWithProof,
  ExtractedTagCandidate,
  TransactionValidationResult,
  TransactionExecutionResult
} from '../types/inventory';
import { SerialRangeHelper } from '../utils/serialRangeHelper';

const STORAGE_KEYS = {
  TAGS: 'fastag_inventory_tags',
  EXECUTIVES: 'fastag_executives',
  PROOFS: 'fastag_proof_documents',
  LEDGER: 'fastag_stock_ledger'
};

const INITIAL_EXECUTIVES: ExecutiveEntity[] = [
  { executiveId: 'EXEC-01', executiveName: 'Kumar S.', region: 'North Corridor', active: true },
  { executiveId: 'EXEC-02', executiveName: 'Ravi Sharma', region: 'South Hub', active: true },
  { executiveId: 'EXEC-03', executiveName: 'Suresh Patel', region: 'West Tollways', active: true },
  { executiveId: 'EXEC-04', executiveName: 'Anil Verma', region: 'East Expressway', active: true },
  { executiveId: 'EXEC-05', executiveName: 'Pooja Reddy', region: 'Metro Ring Road', active: true },
  { executiveId: 'EXEC-06', executiveName: 'Deepak Joshi', region: 'Port Logistics', active: true },
  { executiveId: 'EXEC-07', executiveName: 'Manoj Nair', region: 'Central Hub', active: true },
  { executiveId: 'EXEC-08', executiveName: 'Kavita Rao', region: 'Airport Corridor', active: true },
  { executiveId: 'EXEC-09', executiveName: 'Vikram Singh', region: 'Highway NH-48', active: true }
];

type ChangeListener = () => void;

class InventoryDatabase {
  private listeners: Set<ChangeListener> = new Set();

  private tags: Map<string, InventoryTagEntity> = new Map();
  private executives: Map<string, ExecutiveEntity> = new Map();
  private proofs: ProofDocumentEntity[] = [];
  private ledger: StockMovementLedgerEntity[] = [];

  constructor() {
    this.loadFromStorage();
  }

  subscribe(listener: ChangeListener): () => void {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  }

  private notify() {
    this.saveToStorage();
    this.listeners.forEach(l => l());
  }

  private loadFromStorage() {
    try {
      const storedExecs = localStorage.getItem(STORAGE_KEYS.EXECUTIVES);
      if (storedExecs) {
        const list: ExecutiveEntity[] = JSON.parse(storedExecs);
        this.executives = new Map(list.map(e => [e.executiveId, e]));
      } else {
        this.executives = new Map(INITIAL_EXECUTIVES.map(e => [e.executiveId, e]));
        localStorage.setItem(STORAGE_KEYS.EXECUTIVES, JSON.stringify(INITIAL_EXECUTIVES));
      }

      const storedTags = localStorage.getItem(STORAGE_KEYS.TAGS);
      if (storedTags) {
        const list: InventoryTagEntity[] = JSON.parse(storedTags);
        this.tags = new Map(list.map(t => [t.tagSerialNumber, t]));
      }

      const storedProofs = localStorage.getItem(STORAGE_KEYS.PROOFS);
      if (storedProofs) {
        this.proofs = JSON.parse(storedProofs);
      }

      const storedLedger = localStorage.getItem(STORAGE_KEYS.LEDGER);
      if (storedLedger) {
        this.ledger = JSON.parse(storedLedger);
      }
    } catch (e) {
      console.error('Error loading inventory database from localStorage:', e);
    }
  }

  private saveToStorage() {
    try {
      localStorage.setItem(STORAGE_KEYS.EXECUTIVES, JSON.stringify(Array.from(this.executives.values())));
      localStorage.setItem(STORAGE_KEYS.TAGS, JSON.stringify(Array.from(this.tags.values())));
      localStorage.setItem(STORAGE_KEYS.PROOFS, JSON.stringify(this.proofs));
      localStorage.setItem(STORAGE_KEYS.LEDGER, JSON.stringify(this.ledger));
    } catch (e) {
      console.error('Error saving inventory database to localStorage:', e);
    }
  }

  // --- Read Operations ---

  getAllTags(): InventoryTagEntity[] {
    return Array.from(this.tags.values());
  }

  getTagBySerial(serial: string): InventoryTagEntity | undefined {
    return this.tags.get(serial.trim().toUpperCase());
  }

  getAllExecutives(): ExecutiveEntity[] {
    return Array.from(this.executives.values());
  }

  getActiveExecutives(): ExecutiveEntity[] {
    return Array.from(this.executives.values()).filter(e => e.active);
  }

  getAllProofs(): ProofDocumentEntity[] {
    return [...this.proofs].sort((a, b) => b.uploadedAt - a.uploadedAt);
  }

  getProofById(id: number): ProofDocumentEntity | undefined {
    return this.proofs.find(p => p.id === id);
  }

  getAllLedgerMovements(): StockMovementLedgerEntity[] {
    return [...this.ledger].sort((a, b) => b.createdAt - a.createdAt);
  }

  getRecentMovementsWithProof(limit = 20): MovementWithProof[] {
    const sorted = [...this.ledger].sort((a, b) => b.createdAt - a.createdAt).slice(0, limit);
    return sorted.map(m => {
      const proof = this.proofs.find(p => p.id === m.proofDocumentId);
      return {
        ...m,
        originalFilename: proof?.originalFilename,
        proofType: proof?.proofType,
        uploadedAt: proof?.uploadedAt
      };
    });
  }

  getMovementsForTag(serial: string): StockMovementLedgerEntity[] {
    return this.ledger
      .filter(m => m.tagSerialNumber === serial.toUpperCase())
      .sort((a, b) => a.createdAt - b.createdAt);
  }

  getTagsForExecutive(executiveId: string): InventoryTagEntity[] {
    return Array.from(this.tags.values()).filter(t => t.assignedExecutiveId === executiveId);
  }

  getOverallMetrics(): OverallStockMetrics {
    let centralStock = 0;
    let masterStock = 0;
    let executiveStock = 0;
    let assignedStock = 0;

    for (const tag of this.tags.values()) {
      switch (tag.currentLocation) {
        case 'CENTRAL':
          centralStock++;
          break;
        case 'MASTER':
          masterStock++;
          break;
        case 'EXECUTIVE':
          executiveStock++;
          break;
        case 'ASSIGNED':
          assignedStock++;
          break;
      }
    }

    return {
      centralStock,
      masterStock,
      executiveStock,
      assignedStock,
      totalActive: centralStock + masterStock + executiveStock + assignedStock
    };
  }

  getClassStockSummaries(): ClassStockSummary[] {
    const classMap: Record<string, { central: number; master: number; executive: number; assigned: number }> = {};

    for (const tag of this.tags.values()) {
      const cls = tag.tagClass || 'Unassigned';
      if (!classMap[cls]) {
        classMap[cls] = { central: 0, master: 0, executive: 0, assigned: 0 };
      }
      switch (tag.currentLocation) {
        case 'CENTRAL':
          classMap[cls].central++;
          break;
        case 'MASTER':
          classMap[cls].master++;
          break;
        case 'EXECUTIVE':
          classMap[cls].executive++;
          break;
        case 'ASSIGNED':
          classMap[cls].assigned++;
          break;
      }
    }

    const classes = Object.keys(classMap).sort();
    return classes.map(cls => {
      const row = classMap[cls];
      const total = row.central + row.master + row.executive + row.assigned;
      return {
        tagClass: cls,
        centralCount: row.central,
        masterCount: row.master,
        executiveCount: row.executive,
        assignedCount: row.assigned,
        totalCount: total
      };
    });
  }

  getExecutiveStockSummaries(): ExecutiveStockSummary[] {
    const execs = this.getAllExecutives();
    const countMap: Record<string, number> = {};

    for (const tag of this.tags.values()) {
      if (tag.currentLocation === 'EXECUTIVE' && tag.assignedExecutiveId) {
        countMap[tag.assignedExecutiveId] = (countMap[tag.assignedExecutiveId] || 0) + 1;
      }
    }

    return execs.map(e => ({
      executiveId: e.executiveId,
      executiveName: e.executiveName,
      region: e.region,
      active: e.active,
      currentStock: countMap[e.executiveId] || 0
    }));
  }

  searchTags(query: string, locationFilter = '', classFilter = ''): InventoryTagEntity[] {
    const q = query.trim().toUpperCase();
    return Array.from(this.tags.values()).filter(tag => {
      if (locationFilter && tag.currentLocation !== locationFilter) return false;
      if (classFilter && tag.tagClass !== classFilter) return false;
      if (!q) return true;

      const serialMatches = tag.tagSerialNumber.toUpperCase().includes(q);
      const vehicleMatches = tag.vehicleNumber?.toUpperCase().includes(q) || false;
      const execMatches = tag.assignedExecutiveId?.toUpperCase().includes(q) || false;
      return serialMatches || vehicleMatches || execMatches;
    });
  }

  // --- Validation Engine ---

  validateCandidates(
    proofType: ProofType,
    candidates: ExtractedTagCandidate[],
    targetExecutiveId?: string,
    targetLocationForExisting: InventoryLocation = 'MASTER'
  ): TransactionValidationResult {
    const candidateSerials: string[] = [];
    const classBreakdown: Record<string, number> = {};
    const seenSet = new Set<string>();
    let duplicateWithinBatch = 0;

    for (const c of candidates) {
      const cls = c.tagClass || 'Class 12';
      if (c.isRange) {
        const val = SerialRangeHelper.validateRange(c.fromSerial, c.toSerial);
        if (!val.valid) {
          return {
            isValid: false,
            totalCount: 0,
            classBreakdown: {},
            candidateSerials: [],
            duplicateCount: 0,
            invalidTransitionCount: 0,
            errorMessage: `Invalid range: ${c.fromSerial} to ${c.toSerial} (${val.error})`
          };
        }
        const serials = SerialRangeHelper.expandRange(c.fromSerial, c.toSerial);
        classBreakdown[cls] = (classBreakdown[cls] || 0) + serials.length;

        for (const s of serials) {
          if (seenSet.has(s)) {
            duplicateWithinBatch++;
          } else {
            seenSet.add(s);
            candidateSerials.push(s);
          }
        }
      } else {
        const s = c.serialNumber.trim().toUpperCase();
        if (!s) continue;
        classBreakdown[cls] = (classBreakdown[cls] || 0) + 1;
        if (seenSet.has(s)) {
          duplicateWithinBatch++;
        } else {
          seenSet.add(s);
          candidateSerials.push(s);
        }
      }
    }

    if (candidateSerials.length === 0) {
      return {
        isValid: false,
        totalCount: 0,
        classBreakdown: {},
        candidateSerials: [],
        duplicateCount: 0,
        invalidTransitionCount: 0,
        errorMessage: 'No valid tags or serial numbers found to process.'
      };
    }

    if (duplicateWithinBatch > 0) {
      return {
        isValid: false,
        totalCount: candidateSerials.length,
        classBreakdown,
        candidateSerials,
        duplicateCount: duplicateWithinBatch,
        invalidTransitionCount: 0,
        errorMessage: `Batch contains ${duplicateWithinBatch} duplicate serial number(s) within the document.`
      };
    }

    // State transition validations
    let existingInDbCount = 0;
    let invalidTransitionCount = 0;
    const invalidSampleSerials: string[] = [];

    for (const s of candidateSerials) {
      const existingTag = this.tags.get(s);

      if (proofType === 'EXISTING_STOCK' || proofType === 'CENTRAL_RECEIVED') {
        // Tag must NOT already exist in DB
        if (existingTag) {
          existingInDbCount++;
          if (invalidSampleSerials.length < 3) invalidSampleSerials.push(s);
        }
      } else if (proofType === 'COURIER_TO_EXECUTIVE') {
        // Tag MUST already exist in MASTER
        if (!existingTag || existingTag.currentLocation !== 'MASTER') {
          invalidTransitionCount++;
          if (invalidSampleSerials.length < 3) invalidSampleSerials.push(s);
        }
      } else if (proofType === 'TAG_ASSIGNED') {
        // Tag must be in EXECUTIVE (or MASTER), and NOT already ASSIGNED
        if (!existingTag) {
          invalidTransitionCount++;
          if (invalidSampleSerials.length < 3) invalidSampleSerials.push(`${s} (Not in database)`);
        } else if (existingTag.currentLocation === 'ASSIGNED') {
          invalidTransitionCount++;
          if (invalidSampleSerials.length < 3) invalidSampleSerials.push(`${s} (Already assigned)`);
        }
      }
    }

    if (proofType === 'EXISTING_STOCK' || proofType === 'CENTRAL_RECEIVED') {
      if (existingInDbCount > 0) {
        return {
          isValid: false,
          totalCount: candidateSerials.length,
          classBreakdown,
          candidateSerials,
          duplicateCount: existingInDbCount,
          invalidTransitionCount: 0,
          errorMessage: `REJECTED: ${existingInDbCount} tag(s) already exist in database (e.g. ${invalidSampleSerials.join(', ')}). Inward stock cannot contain existing tags.`
        };
      }
    } else if (proofType === 'COURIER_TO_EXECUTIVE') {
      if (!targetExecutiveId) {
        return {
          isValid: false,
          totalCount: candidateSerials.length,
          classBreakdown,
          candidateSerials,
          duplicateCount: 0,
          invalidTransitionCount: 0,
          errorMessage: 'Target executive / subagent must be selected for courier dispatch.'
        };
      }
      if (invalidTransitionCount > 0) {
        return {
          isValid: false,
          totalCount: candidateSerials.length,
          classBreakdown,
          candidateSerials,
          duplicateCount: 0,
          invalidTransitionCount,
          errorMessage: `REJECTED: ${invalidTransitionCount} tag(s) cannot be moved (e.g. ${invalidSampleSerials.join(', ')}). All tags must currently reside in MASTER inventory.`
        };
      }
    } else if (proofType === 'TAG_ASSIGNED') {
      if (invalidTransitionCount > 0) {
        return {
          isValid: false,
          totalCount: candidateSerials.length,
          classBreakdown,
          candidateSerials,
          duplicateCount: 0,
          invalidTransitionCount,
          errorMessage: `REJECTED: ${invalidTransitionCount} tag(s) cannot be assigned (e.g. ${invalidSampleSerials.join(', ')}). Tags must be in active executive stock and not previously installed.`
        };
      }
    }

    return {
      isValid: true,
      totalCount: candidateSerials.length,
      classBreakdown,
      candidateSerials,
      duplicateCount: 0,
      invalidTransitionCount: 0,
      errorMessage: null
    };
  }

  // --- Atomic Transaction Execution ---

  executeTransaction(
    proofType: ProofType,
    candidates: ExtractedTagCandidate[],
    documentMeta: {
      originalFilename: string;
      rawText: string;
      uploadedBy: string;
      vehicleNumber?: string;
      targetExecutiveId?: string;
      targetLocationForExisting?: InventoryLocation;
    }
  ): TransactionExecutionResult {
    // 1. Validate
    const validation = this.validateCandidates(
      proofType,
      candidates,
      documentMeta.targetExecutiveId,
      documentMeta.targetLocationForExisting
    );

    if (!validation.isValid) {
      return {
        success: false,
        proofId: 0,
        affectedCount: 0,
        message: validation.errorMessage || 'Transaction validation failed.'
      };
    }

    const timestamp = Date.now();
    const proofId = this.proofs.length > 0 ? Math.max(...this.proofs.map(p => p.id)) + 1 : 1;

    // 2. Create Proof Document
    const newProof: ProofDocumentEntity = {
      id: proofId,
      proofType,
      originalFilename: documentMeta.originalFilename || `manifest_${proofType.toLowerCase()}_${proofId}.pdf`,
      storageObjectKey: `proofs/${proofType.toLowerCase()}/${Date.now()}_ref${proofId}.enc`,
      uploadedAt: timestamp,
      uploadedBy: documentMeta.uploadedBy || 'Admin',
      rawExtractedText: documentMeta.rawText,
      fileSize: documentMeta.rawText.length * 2 + 1024
    };

    // 3. Process each candidate and update records
    let targetLoc: InventoryLocation;
    if (proofType === 'EXISTING_STOCK') {
      targetLoc = documentMeta.targetLocationForExisting || 'MASTER';
    } else if (proofType === 'CENTRAL_RECEIVED') {
      targetLoc = 'MASTER';
    } else if (proofType === 'COURIER_TO_EXECUTIVE') {
      targetLoc = 'EXECUTIVE';
    } else if (proofType === 'TAG_ASSIGNED') {
      targetLoc = 'ASSIGNED';
    } else {
      targetLoc = 'ASSIGNED';
    }

    const targetExec = documentMeta.targetExecutiveId
      ? this.executives.get(documentMeta.targetExecutiveId)
      : undefined;

    let affectedCount = 0;

    for (const c of candidates) {
      const cls = c.tagClass || 'Class 12';
      const serials = c.isRange
        ? SerialRangeHelper.expandRange(c.fromSerial, c.toSerial)
        : [c.serialNumber.trim().toUpperCase()];

      for (const s of serials) {
        const existing = this.tags.get(s);
        const prevLoc = existing ? existing.currentLocation : (proofType === 'CENTRAL_RECEIVED' ? 'CENTRAL' : null);

        const updatedTag: InventoryTagEntity = {
          tagSerialNumber: s,
          tagClass: cls,
          currentLocation: targetLoc,
          assignedExecutiveId: targetLoc === 'EXECUTIVE'
            ? (targetExec?.executiveId || c.subagentId || null)
            : (existing?.assignedExecutiveId || null),
          vehicleNumber: documentMeta.vehicleNumber || existing?.vehicleNumber || null,
          lastProofDocumentId: proofId,
          updatedAt: timestamp
        };

        this.tags.set(s, updatedTag);

        // Append to ledger
        const ledgerId = this.ledger.length > 0 ? Math.max(...this.ledger.map(l => l.id)) + 1 : 1;
        this.ledger.push({
          id: ledgerId,
          tagSerialNumber: s,
          tagClass: cls,
          previousLocation: prevLoc,
          newLocation: targetLoc,
          subagentId: targetExec?.executiveId || c.subagentId || 'CENTRAL',
          subagentName: targetExec?.executiveName || c.subagentName || 'Central Hub',
          proofDocumentId: proofId,
          createdAt: timestamp
        });

        affectedCount++;
      }
    }

    this.proofs.push(newProof);
    this.notify();

    return {
      success: true,
      proofId,
      affectedCount,
      message: `Successfully verified and committed ${affectedCount} tag(s) to ${targetLoc} location with Proof #${proofId}.`
    };
  }

  // --- Executive Management ---

  addExecutive(exec: Omit<ExecutiveEntity, 'active'>): boolean {
    const id = exec.executiveId.trim().toUpperCase();
    if (this.executives.has(id)) {
      return false;
    }
    this.executives.set(id, { ...exec, executiveId: id, active: true });
    this.notify();
    return true;
  }

  toggleExecutiveStatus(executiveId: string): boolean {
    const exec = this.executives.get(executiveId);
    if (!exec) return false;
    exec.active = !exec.active;
    this.notify();
    return true;
  }

  // --- Reset DB for Acceptance Tests ---

  resetDatabase() {
    this.tags.clear();
    this.proofs = [];
    this.ledger = [];
    this.executives = new Map(INITIAL_EXECUTIVES.map(e => [e.executiveId, { ...e }]));
    this.notify();
  }
}

export const db = new InventoryDatabase();
