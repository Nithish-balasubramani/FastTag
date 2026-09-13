/**
 * Serial Range Helper
 * Matches Android SerialRangeHelper.kt logic for expanding contiguous FASTag serial ranges.
 */

export interface RangeValidationResult {
  valid: boolean;
  error?: string;
  count: number;
}

export class SerialRangeHelper {
  static validateRange(from: string, to: string, maxAllowed = 1000): RangeValidationResult {
    const cleanFrom = from.trim().toUpperCase();
    const cleanTo = to.trim().toUpperCase();

    if (!cleanFrom || !cleanTo) {
      return { valid: false, error: 'Start and end serials cannot be blank.', count: 0 };
    }

    if (cleanFrom.length !== cleanTo.length) {
      return {
        valid: false,
        error: `Serial length mismatch: '${cleanFrom}' (${cleanFrom.length} chars) vs '${cleanTo}' (${cleanTo.length} chars).`,
        count: 0
      };
    }

    // Pure numeric range
    if (/^\d+$/.test(cleanFrom) && /^\d+$/.test(cleanTo)) {
      try {
        const start = BigInt(cleanFrom);
        const end = BigInt(cleanTo);

        if (start > end) {
          return { valid: false, error: `Start serial (${cleanFrom}) cannot be greater than end serial (${cleanTo}).`, count: 0 };
        }

        const count = Number(end - start + 1n);
        if (count > maxAllowed) {
          return { valid: false, error: `Range size (${count.toLocaleString()}) exceeds maximum allowed batch limit of ${maxAllowed}.`, count };
        }

        return { valid: true, count };
      } catch {
        return { valid: false, error: 'Invalid numeric serial range format.', count: 0 };
      }
    }

    // Alpha-numeric prefix with numeric suffix
    const fromMatch = cleanFrom.match(/^([A-Za-z0-9_-]*?)(\d+)$/);
    const toMatch = cleanTo.match(/^([A-Za-z0-9_-]*?)(\d+)$/);

    if (fromMatch && toMatch) {
      const [, prefixFrom, digitsFrom] = fromMatch;
      const [, prefixTo, digitsTo] = toMatch;

      if (prefixFrom !== prefixTo) {
        return { valid: false, error: `Prefix mismatch: '${prefixFrom}' vs '${prefixTo}'.`, count: 0 };
      }

      if (digitsFrom.length !== digitsTo.length) {
        return { valid: false, error: 'Suffix numeric length mismatch.', count: 0 };
      }

      try {
        const start = BigInt(digitsFrom);
        const end = BigInt(digitsTo);

        if (start > end) {
          return { valid: false, error: `Start number (${digitsFrom}) cannot exceed end number (${digitsTo}).`, count: 0 };
        }

        const count = Number(end - start + 1n);
        if (count > maxAllowed) {
          return { valid: false, error: `Range size (${count.toLocaleString()}) exceeds maximum allowed limit of ${maxAllowed}.`, count };
        }

        return { valid: true, count };
      } catch {
        return { valid: false, error: 'Failed to compute alphanumeric serial range.', count: 0 };
      }
    }

    return { valid: false, error: 'Unrecognized serial format. Must end with numeric sequence.', count: 0 };
  }

  static expandRange(from: string, to: string, maxAllowed = 1000): string[] {
    const validation = this.validateRange(from, to, maxAllowed);
    if (!validation.valid) {
      throw new Error(validation.error || 'Invalid range');
    }

    const cleanFrom = from.trim().toUpperCase();
    const cleanTo = to.trim().toUpperCase();

    if (/^\d+$/.test(cleanFrom) && /^\d+$/.test(cleanTo)) {
      const len = cleanFrom.length;
      const start = BigInt(cleanFrom);
      const end = BigInt(cleanTo);
      const result: string[] = [];

      for (let curr = start; curr <= end; curr++) {
        result.push(curr.toString().padStart(len, '0'));
      }
      return result;
    }

    const fromMatch = cleanFrom.match(/^([A-Za-z0-9_-]*?)(\d+)$/)!;
    const toMatch = cleanTo.match(/^([A-Za-z0-9_-]*?)(\d+)$/)!;
    const prefix = fromMatch[1];
    const digitsFrom = fromMatch[2];
    const digitsTo = toMatch[2];
    const len = digitsFrom.length;

    const start = BigInt(digitsFrom);
    const end = BigInt(digitsTo);
    const result: string[] = [];

    for (let curr = start; curr <= end; curr++) {
      result.push(prefix + curr.toString().padStart(len, '0'));
    }
    return result;
  }
}
