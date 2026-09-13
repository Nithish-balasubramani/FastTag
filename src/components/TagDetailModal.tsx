import React from 'react';
import { db } from '../data/db';
import { LocationBadge } from './LocationBadge';
import { X, Tag, Truck, User, Calendar, ShieldCheck, History } from 'lucide-react';

interface TagDetailModalProps {
  serial: string;
  onClose: () => void;
  onInspectProof?: (proofId: number) => void;
}

export const TagDetailModal: React.FC<TagDetailModalProps> = ({ serial, onClose, onInspectProof }) => {
  const tag = db.getTagBySerial(serial);
  const movements = db.getMovementsForTag(serial);
  const exec = tag?.assignedExecutiveId ? db.getAllExecutives().find(e => e.executiveId === tag.assignedExecutiveId) : null;

  if (!tag) {
    return (
      <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-xs p-4">
        <div className="bg-slate-800 border border-slate-700 rounded-xl p-6 max-w-md w-full">
          <h3 className="text-lg font-bold text-white mb-2">Tag Not Found</h3>
          <p className="text-slate-400 text-sm mb-4">No RFID tag found with serial: <span className="font-mono text-amber-400">{serial}</span></p>
          <button onClick={onClose} className="w-full py-2 bg-slate-700 hover:bg-slate-600 rounded-lg text-white font-medium text-sm">
            Close
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-xs p-4">
      <div className="bg-slate-800 border border-slate-700 rounded-xl max-w-2xl w-full max-h-[90vh] flex flex-col shadow-2xl overflow-hidden">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-700 bg-slate-800/80">
          <div className="flex items-center space-x-3">
            <div className="p-2 bg-sky-500/10 border border-sky-500/20 rounded-lg text-sky-400">
              <Tag className="w-5 h-5" />
            </div>
            <div>
              <div className="flex items-center space-x-2">
                <span className="font-mono font-bold text-lg text-white tracking-wider">{tag.tagSerialNumber}</span>
                <span className="px-2 py-0.5 rounded text-xs font-semibold bg-slate-700 text-slate-300">{tag.tagClass}</span>
              </div>
              <p className="text-xs text-slate-400">RFID Electronic Toll Collection Tag</p>
            </div>
          </div>
          <button onClick={onClose} className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-700 transition">
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content */}
        <div className="p-6 overflow-y-auto space-y-6">
          {/* Status grid */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
            <div className="bg-slate-900/60 p-3 rounded-lg border border-slate-700/50">
              <div className="text-xs text-slate-400 mb-1">Current Location</div>
              <LocationBadge location={tag.currentLocation} />
            </div>

            <div className="bg-slate-900/60 p-3 rounded-lg border border-slate-700/50">
              <div className="text-xs text-slate-400 mb-1">Assigned Executive</div>
              <div className="text-sm font-semibold text-white truncate">
                {exec ? `${exec.executiveName} (${exec.executiveId})` : (tag.assignedExecutiveId || 'None')}
              </div>
            </div>

            <div className="bg-slate-900/60 p-3 rounded-lg border border-slate-700/50">
              <div className="text-xs text-slate-400 mb-1">Vehicle Attached</div>
              <div className="text-sm font-mono font-semibold text-emerald-400 truncate">
                {tag.vehicleNumber || 'Unassigned'}
              </div>
            </div>

            <div className="bg-slate-900/60 p-3 rounded-lg border border-slate-700/50">
              <div className="text-xs text-slate-400 mb-1">Last Proof Ref</div>
              <button
                onClick={() => onInspectProof?.(tag.lastProofDocumentId)}
                className="text-xs font-mono text-sky-400 hover:text-sky-300 flex items-center underline"
              >
                Proof #{tag.lastProofDocumentId}
              </button>
            </div>
          </div>

          {/* Chronological Audit Ledger */}
          <div>
            <div className="flex items-center space-x-2 text-sm font-bold text-white mb-3">
              <History className="w-4 h-4 text-sky-400" />
              <span>IMMUTABLE STOCK MOVEMENT LEDGER</span>
            </div>

            {movements.length === 0 ? (
              <p className="text-sm text-slate-400 italic">No movement records found.</p>
            ) : (
              <div className="relative pl-6 space-y-4 before:absolute before:left-2.5 before:top-2 before:bottom-2 before:w-0.5 before:bg-slate-700">
                {movements.map((m, idx) => (
                  <div key={m.id} className="relative">
                    <div className="absolute -left-6 top-1 w-3 h-3 rounded-full bg-sky-500 border-2 border-slate-800" />
                    <div className="bg-slate-900/50 border border-slate-700/60 rounded-lg p-3">
                      <div className="flex items-center justify-between text-xs mb-1.5">
                        <span className="font-semibold text-slate-300">
                          {m.previousLocation ? `${m.previousLocation} ➔ ${m.newLocation}` : `Initial Entry ➔ ${m.newLocation}`}
                        </span>
                        <span className="text-slate-400 font-mono">
                          {new Date(m.createdAt).toLocaleString()}
                        </span>
                      </div>
                      <div className="flex items-center justify-between text-xs text-slate-400">
                        <div className="flex items-center space-x-1.5">
                          <User className="w-3.5 h-3.5 text-slate-500" />
                          <span>Subagent: <strong className="text-slate-300">{m.subagentName} ({m.subagentId})</strong></span>
                        </div>
                        <button
                          onClick={() => onInspectProof?.(m.proofDocumentId)}
                          className="text-sky-400 hover:text-sky-300 font-mono flex items-center space-x-1"
                        >
                          <ShieldCheck className="w-3.5 h-3.5" />
                          <span>Proof Doc #{m.proofDocumentId}</span>
                        </button>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        {/* Footer */}
        <div className="px-6 py-3 border-t border-slate-700 bg-slate-900/50 flex justify-end">
          <button
            onClick={onClose}
            className="px-4 py-2 bg-slate-700 hover:bg-slate-600 rounded-lg text-white font-medium text-sm transition"
          >
            Close Inspector
          </button>
        </div>
      </div>
    </div>
  );
};
