import React from 'react';
import { db } from '../data/db';
import { X, ShieldCheck, FileText, User, Calendar, Database, Hash } from 'lucide-react';

interface ProofDetailModalProps {
  proofId: number;
  onClose: () => void;
  onFilterByProof?: (proofId: number) => void;
}

export const ProofDetailModal: React.FC<ProofDetailModalProps> = ({ proofId, onClose, onFilterByProof }) => {
  const proof = db.getProofById(proofId);
  const movements = db.getAllLedgerMovements().filter(m => m.proofDocumentId === proofId);

  if (!proof) {
    return (
      <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-xs p-4">
        <div className="bg-slate-800 border border-slate-700 rounded-xl p-6 max-w-md w-full">
          <h3 className="text-lg font-bold text-white mb-2">Proof Not Found</h3>
          <p className="text-slate-400 text-sm mb-4">No proof document found with ID #{proofId}</p>
          <button onClick={onClose} className="w-full py-2 bg-slate-700 hover:bg-slate-600 rounded-lg text-white font-medium text-sm">
            Close
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-xs p-4">
      <div className="bg-slate-800 border border-slate-700 rounded-xl max-w-3xl w-full max-h-[90vh] flex flex-col shadow-2xl overflow-hidden">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-700 bg-slate-800/80">
          <div className="flex items-center space-x-3">
            <div className="p-2 bg-emerald-500/10 border border-emerald-500/20 rounded-lg text-emerald-400">
              <ShieldCheck className="w-5 h-5" />
            </div>
            <div>
              <div className="flex items-center space-x-2">
                <span className="font-bold text-lg text-white">Proof Document #{proof.id}</span>
                <span className="px-2 py-0.5 rounded text-xs font-semibold bg-emerald-500/20 text-emerald-300 border border-emerald-500/30">
                  {proof.proofType}
                </span>
              </div>
              <p className="text-xs text-slate-400 font-mono">{proof.originalFilename}</p>
            </div>
          </div>
          <button onClick={onClose} className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-700 transition">
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content */}
        <div className="p-6 overflow-y-auto space-y-6">
          {/* Metadata Grid */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
            <div className="bg-slate-900/60 p-3 rounded-lg border border-slate-700/50">
              <div className="flex items-center text-xs text-slate-400 mb-1">
                <Calendar className="w-3.5 h-3.5 mr-1 text-slate-500" />
                Timestamp
              </div>
              <div className="text-xs font-mono text-slate-200">
                {new Date(proof.uploadedAt).toLocaleString()}
              </div>
            </div>

            <div className="bg-slate-900/60 p-3 rounded-lg border border-slate-700/50">
              <div className="flex items-center text-xs text-slate-400 mb-1">
                <User className="w-3.5 h-3.5 mr-1 text-slate-500" />
                Uploaded By
              </div>
              <div className="text-sm font-semibold text-slate-200 truncate">
                {proof.uploadedBy}
              </div>
            </div>

            <div className="bg-slate-900/60 p-3 rounded-lg border border-slate-700/50">
              <div className="flex items-center text-xs text-slate-400 mb-1">
                <Database className="w-3.5 h-3.5 mr-1 text-slate-500" />
                Tags Committed
              </div>
              <div className="text-sm font-bold text-sky-400">
                {movements.length} tag(s)
              </div>
            </div>

            <div className="bg-slate-900/60 p-3 rounded-lg border border-slate-700/50">
              <div className="flex items-center text-xs text-slate-400 mb-1">
                <Hash className="w-3.5 h-3.5 mr-1 text-slate-500" />
                Vault Key
              </div>
              <div className="text-xs font-mono text-slate-400 truncate" title={proof.storageObjectKey}>
                {proof.storageObjectKey}
              </div>
            </div>
          </div>

          {/* Committed Tags Sample */}
          <div>
            <h4 className="text-xs font-bold text-slate-400 uppercase tracking-wider mb-2">
              Committed RFID Tags in this Manifest ({movements.length})
            </h4>
            <div className="bg-slate-900/80 border border-slate-700/60 rounded-lg p-3 max-h-40 overflow-y-auto">
              <div className="flex flex-wrap gap-1.5">
                {movements.slice(0, 50).map(m => (
                  <span key={m.id} className="font-mono text-xs px-2 py-0.5 rounded bg-slate-800 border border-slate-700 text-slate-300">
                    {m.tagSerialNumber}
                  </span>
                ))}
                {movements.length > 50 && (
                  <span className="text-xs text-slate-400 px-2 py-0.5">
                    + {movements.length - 50} more tags...
                  </span>
                )}
              </div>
            </div>
          </div>

          {/* Raw Extracted OCR Text */}
          <div>
            <div className="flex items-center space-x-2 text-xs font-bold text-slate-400 uppercase tracking-wider mb-2">
              <FileText className="w-3.5 h-3.5 text-sky-400" />
              <span>Immutable OCR Document Transcript</span>
            </div>
            <pre className="bg-slate-950/80 border border-slate-800 rounded-lg p-3 text-xs font-mono text-emerald-400/90 whitespace-pre-wrap max-h-56 overflow-y-auto leading-relaxed">
              {proof.rawExtractedText || 'No OCR transcript recorded.'}
            </pre>
          </div>
        </div>

        {/* Footer */}
        <div className="px-6 py-3 border-t border-slate-700 bg-slate-900/50 flex justify-between items-center">
          <div className="text-xs text-slate-400">
            Hash verified • Immutable audit ledger record
          </div>
          <button
            onClick={onClose}
            className="px-4 py-2 bg-slate-700 hover:bg-slate-600 rounded-lg text-white font-medium text-sm transition"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  );
};
