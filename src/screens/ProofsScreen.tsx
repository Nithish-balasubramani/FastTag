import React, { useState, useMemo } from 'react';
import { db } from '../data/db';
import { ProofDocumentEntity, ProofType } from '../types/inventory';
import { ShieldCheck, Receipt, FileText, Calendar, User, Eye, Hash, Filter } from 'lucide-react';

interface ProofsScreenProps {
  onInspectProof: (proofId: number) => void;
}

export const ProofsScreen: React.FC<ProofsScreenProps> = ({ onInspectProof }) => {
  const [selectedTypeFilter, setSelectedTypeFilter] = useState<string>('');
  const proofs = db.getAllProofs();

  const proofTypes: { type: ProofType; label: string }[] = [
    { type: 'EXISTING_STOCK', label: 'Existing Stock' },
    { type: 'CENTRAL_RECEIVED', label: 'Central Inward' },
    { type: 'COURIER_TO_EXECUTIVE', label: 'Courier Dispatch' },
    { type: 'TAG_ASSIGNED', label: 'Tag Fitment' },
    { type: 'USER_TAG_UPLOAD', label: 'Field Camera Snap' }
  ];

  const filteredProofs = useMemo(() => {
    if (!selectedTypeFilter) return proofs;
    return proofs.filter(p => p.proofType === selectedTypeFilter);
  }, [proofs, selectedTypeFilter]);

  return (
    <div className="space-y-6 max-w-6xl mx-auto">
      {/* Header */}
      <div>
        <h1 className="text-lg font-bold text-white tracking-tight flex items-center space-x-2">
          <ShieldCheck className="w-5 h-5 text-emerald-400" />
          <span>PROOF DOCUMENTS VAULT</span>
        </h1>
        <p className="text-xs text-slate-400 mt-0.5">
          Secure, immutable digital repository of all scanned manifests, challans, and waybills.
        </p>
      </div>

      {/* Filter Chips */}
      <div className="flex items-center gap-1.5 overflow-x-auto pb-1 scrollbar-none">
        <span className="text-[11px] font-semibold text-slate-400 mr-1 flex items-center">
          <Filter className="w-3 h-3 mr-1" /> Type:
        </span>
        <button
          onClick={() => setSelectedTypeFilter('')}
          className={`px-3 py-1 rounded-full text-xs font-semibold whitespace-nowrap transition ${
            selectedTypeFilter === ''
              ? 'bg-emerald-600 text-white shadow-xs'
              : 'bg-slate-800 text-slate-300 hover:bg-slate-750'
          }`}
        >
          All Proofs ({proofs.length})
        </button>
        {proofTypes.map(item => {
          const count = proofs.filter(p => p.proofType === item.type).length;
          const isSelected = selectedTypeFilter === item.type;
          return (
            <button
              key={item.type}
              onClick={() => setSelectedTypeFilter(isSelected ? '' : item.type)}
              className={`px-3 py-1 rounded-full text-xs font-semibold whitespace-nowrap transition ${
                isSelected
                  ? 'bg-emerald-600 text-white shadow-xs'
                  : 'bg-slate-800 text-slate-300 hover:bg-slate-750'
              }`}
            >
              {item.label} ({count})
            </button>
          );
        })}
      </div>

      {/* Proofs List */}
      {filteredProofs.length === 0 ? (
        <div className="bg-slate-800/40 border border-slate-700/60 rounded-2xl p-12 text-center">
          <Receipt className="w-10 h-10 text-slate-600 mx-auto mb-2" />
          <h3 className="text-sm font-bold text-slate-300">No proof documents recorded</h3>
          <p className="text-xs text-slate-500 mt-1 max-w-sm mx-auto">
            Process a waybill in the workflow or run acceptance tests to generate verifiable proof records.
          </p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {filteredProofs.map(proof => (
            <div
              key={proof.id}
              className="bg-slate-800/80 border border-slate-700/70 hover:border-slate-600 rounded-xl p-5 shadow-xs flex flex-col justify-between transition"
            >
              <div>
                <div className="flex items-start justify-between mb-3">
                  <div className="flex items-start space-x-3">
                    <div className="p-2 rounded-xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 mt-0.5">
                      <FileText className="w-5 h-5" />
                    </div>
                    <div>
                      <div className="flex items-center space-x-2">
                        <span className="font-bold text-sm text-white">Proof #{proof.id}</span>
                        <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-emerald-500/15 text-emerald-300 border border-emerald-500/30">
                          {proof.proofType}
                        </span>
                      </div>
                      <p className="text-xs text-slate-300 font-mono mt-0.5 truncate max-w-[280px]">
                        {proof.originalFilename}
                      </p>
                    </div>
                  </div>
                </div>

                <div className="space-y-1.5 text-xs text-slate-400 bg-slate-900/50 p-3 rounded-lg border border-slate-700/50 mb-4">
                  <div className="flex items-center justify-between">
                    <span className="flex items-center">
                      <Calendar className="w-3.5 h-3.5 mr-1.5 text-slate-500" />
                      Uploaded:
                    </span>
                    <span className="font-mono text-slate-200">{new Date(proof.uploadedAt).toLocaleString()}</span>
                  </div>

                  <div className="flex items-center justify-between">
                    <span className="flex items-center">
                      <User className="w-3.5 h-3.5 mr-1.5 text-slate-500" />
                      Auditor:
                    </span>
                    <span className="text-slate-200">{proof.uploadedBy}</span>
                  </div>

                  <div className="flex items-center justify-between">
                    <span className="flex items-center">
                      <Hash className="w-3.5 h-3.5 mr-1.5 text-slate-500" />
                      Vault Ref:
                    </span>
                    <span className="font-mono text-[11px] text-slate-400 truncate max-w-[200px]">
                      {proof.storageObjectKey}
                    </span>
                  </div>
                </div>
              </div>

              <div className="flex items-center justify-between pt-2 border-t border-slate-700/60">
                <span className="text-[11px] text-emerald-400 font-mono">
                  SHA256 Encrypted Archive
                </span>
                <button
                  onClick={() => onInspectProof(proof.id)}
                  className="px-3 py-1.5 bg-slate-700 hover:bg-slate-600 text-slate-200 rounded-lg text-xs font-semibold flex items-center space-x-1.5 transition"
                >
                  <Eye className="w-3.5 h-3.5 text-sky-400" />
                  <span>Inspect Document</span>
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
