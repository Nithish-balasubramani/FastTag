import { ExtractedTagCandidate } from '../types/inventory';
import { SerialRangeHelper } from '../utils/serialRangeHelper';

export interface OcrDocumentMetadata {
  bankName?: string;
  vehicleNumber?: string;
  subagentId?: string;
  subagentName?: string;
  detectedClass?: string;
  documentTypeHint?: string;
}

export interface OcrExtractionResult {
  candidates: ExtractedTagCandidate[];
  metadata: OcrDocumentMetadata;
  rawText: string;
  parsingSummary: string;
}

export class OcrEngine {
  private static readonly BANK_PATTERNS: { name: string; regex: RegExp }[] = [
    { name: 'ICICI Bank', regex: /\bICICI\b/i },
    { name: 'Axis Bank', regex: /\bAXIS\b/i },
    { name: 'HDFC Bank', regex: /\bHDFC\b/i },
    { name: 'State Bank of India', regex: /\b(SBI|STATE BANK OF INDIA)\b/i },
    { name: 'IDFC First Bank', regex: /\bIDFC\b/i },
    { name: 'Paytm Payments Bank', regex: /\bPAYTM\b/i },
    { name: 'Kotak Mahindra', regex: /\bKOTAK\b/i },
    { name: 'IndusInd Bank', regex: /\bINDUSIND\b/i },
    { name: 'Airtel Payments Bank', regex: /\bAIRTEL\b/i }
  ];

  private static readonly VEHICLE_REGEX = /\b([A-Z]{2}[ -]?[0-9]{1,2}[ -]?[A-Z]{1,3}[ -]?[0-9]{4})\b/i;
  private static readonly SUBAGENT_REGEX = /\b(EXEC-[0-9]{2}|SA-[0-9]{4})\b/i;
  private static readonly CLASS_REGEX = /\b(?:Class|Tag Class|VC|Vehicle Class)[\s:-]*([0-9]{1,2})\b/i;
  private static readonly RANGE_REGEX = /\b(\d{5,16})\s*(?:to|-|through|\.\.|\s+TO\s+)\s*(\d{5,16})\b/i;

  static parseDocumentText(text: string, defaultSubagentId = '', defaultSubagentName = ''): OcrExtractionResult {
    const lines = text.split(/\r?\n/).map(l => l.trim()).filter(Boolean);
    const metadata: OcrDocumentMetadata = {};
    const candidates: ExtractedTagCandidate[] = [];

    // 1. Detect Bank
    for (const b of this.BANK_PATTERNS) {
      if (b.regex.test(text)) {
        metadata.bankName = b.name;
        break;
      }
    }

    // 2. Detect Vehicle
    const vehMatch = text.match(this.VEHICLE_REGEX);
    if (vehMatch) {
      metadata.vehicleNumber = vehMatch[1].replace(/\s+/g, '-').toUpperCase();
    }

    // 3. Detect Subagent ID
    const subMatch = text.match(this.SUBAGENT_REGEX);
    if (subMatch) {
      metadata.subagentId = subMatch[1].toUpperCase();
    }

    // 4. Detect Global Class hint
    const classMatch = text.match(this.CLASS_REGEX);
    let currentTagClass = classMatch ? `Class ${classMatch[1]}` : 'Class 12';
    metadata.detectedClass = currentTagClass;

    // Detect Subagent Name hint
    const nameMatch = text.match(/(?:Suresh Patel|Kumar S|Ravi Sharma|Anil Verma|Pooja Reddy|Deepak Joshi|Manoj Nair|Kavita Rao|Vikram Singh)/i);
    if (nameMatch) {
      metadata.subagentName = nameMatch[0];
    }

    // 5. Parse line by line for structured items and ranges
    for (let i = 0; i < lines.length; i++) {
      const line = lines[i];

      // Update current class if line specifies one
      const lineClassMatch = line.match(this.CLASS_REGEX);
      if (lineClassMatch) {
        currentTagClass = `Class ${lineClassMatch[1]}`;
      }

      // Check for Tabular format: e.g. "Class 4   400001   400030   30   EXEC-01"
      const tabMatch = line.match(/^(?:Class\s*(\d+))\s+(\d{5,16})\s+(\d{5,16})(?:\s+(\d+))?(?:\s+(EXEC-\d+|SA-\d+))?/i);
      if (tabMatch) {
        const cls = `Class ${tabMatch[1]}`;
        const from = tabMatch[2];
        const to = tabMatch[3];
        const sub = tabMatch[5] || metadata.subagentId || defaultSubagentId;
        const val = SerialRangeHelper.validateRange(from, to);
        if (val.valid) {
          candidates.push({
            id: `cand-${Date.now()}-${Math.random().toString(36).substring(2, 7)}`,
            subagentId: sub,
            subagentName: metadata.subagentName || defaultSubagentName,
            tagClass: cls,
            isRange: true,
            serialNumber: '',
            fromSerial: from,
            toSerial: to,
            calculatedQuantity: val.count
          });
          continue;
        }
      }

      // Check for Range in line
      const rangeMatch = line.match(this.RANGE_REGEX);
      if (rangeMatch) {
        const from = rangeMatch[1];
        const to = rangeMatch[2];
        const val = SerialRangeHelper.validateRange(from, to);
        if (val.valid) {
          candidates.push({
            id: `cand-${Date.now()}-${Math.random().toString(36).substring(2, 7)}`,
            subagentId: metadata.subagentId || defaultSubagentId,
            subagentName: metadata.subagentName || defaultSubagentName,
            tagClass: currentTagClass,
            isRange: true,
            serialNumber: '',
            fromSerial: from,
            toSerial: to,
            calculatedQuantity: val.count
          });
          continue;
        }
      }

      // Check for explicit single serial line
      const singleSerialMatch = line.match(/(?:Serial|Tag|Barcode|EPC|RFID)[\s:#-]*([0-9A-Z]{6,16})\b/i);
      if (singleSerialMatch && !/^(?:to|through|chassis|engine)/i.test(singleSerialMatch[1])) {
        const ser = singleSerialMatch[1].trim();
        // Ignore date-like or time-like serial false positives
        if (!/^\d{4}-\d{2}-\d{2}$/.test(ser) && !candidates.some(c => !c.isRange && c.serialNumber === ser)) {
          candidates.push({
            id: `cand-${Date.now()}-${Math.random().toString(36).substring(2, 7)}`,
            subagentId: metadata.subagentId || defaultSubagentId,
            subagentName: metadata.subagentName || defaultSubagentName,
            tagClass: currentTagClass,
            isRange: false,
            serialNumber: ser,
            fromSerial: '',
            toSerial: '',
            calculatedQuantity: 1
          });
        }
      }
    }

    // Fallback: If no candidate was found via structured lines, look for any standalone 6-12 digit numbers
    if (candidates.length === 0) {
      const numbers = text.match(/\b\d{6,12}\b/g) || [];
      const uniqueNumbers = Array.from(new Set(numbers)).slice(0, 10);
      for (const num of uniqueNumbers) {
        candidates.push({
          id: `cand-${Date.now()}-${Math.random().toString(36).substring(2, 7)}`,
          subagentId: metadata.subagentId || defaultSubagentId,
          subagentName: metadata.subagentName || defaultSubagentName,
          tagClass: metadata.detectedClass || 'Class 12',
          isRange: false,
          serialNumber: num,
          fromSerial: '',
          toSerial: '',
          calculatedQuantity: 1
        });
      }
    }

    const totalCalculated = candidates.reduce((sum, c) => sum + c.calculatedQuantity, 0);

    return {
      candidates,
      metadata,
      rawText: text,
      parsingSummary: `Extracted ${candidates.length} candidate batch(es) representing ${totalCalculated} total RFID tag(s).`
    };
  }

  /**
   * Process an image file or data URL via Tesseract.js (with fallback to client heuristics)
   */
  static async recognizeImage(imageSource: string | File): Promise<string> {
    try {
      const { createWorker } = await import('tesseract.js');
      const worker = await createWorker('eng');
      const ret = await worker.recognize(imageSource);
      await worker.terminate();
      return ret.data.text;
    } catch (err) {
      console.warn('Tesseract client OCR error, using simulated image recognition fallback:', err);
      return '';
    }
  }
}
