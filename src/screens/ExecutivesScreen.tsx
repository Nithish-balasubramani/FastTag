import React, { useState } from 'react';
import { db } from '../data/db';
import { ExecutiveEntity } from '../types/inventory';
import { Users, Plus, ShieldCheck, Tag, RefreshCw, AlertTriangle, Check, X, MapPin } from 'lucide-react';

interface ExecutivesScreenProps {
  onInspectExecutiveTags: (executiveId: string) => void;
  onOpenAcceptanceTests: () => void;
}

export const ExecutivesScreen: React.FC<ExecutivesScreenProps> = ({
  onInspectExecutiveTags,
  onOpenAcceptanceTests
}) => {
  const [isAddModalOpen, setIsAddModalOpen] = useState(false);
  const [newId, setNewId] = useState('');
  const [newName, setNewName] = useState('');
  const [newRegion, setNewRegion] = useState('');
  const [errorMessage, setErrorMessage] = useState('');

  const execSummaries = db.getExecutiveStockSummaries();

  const handleAddExecutive = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newId.trim() || !newName.trim() || !newRegion.trim()) {
      setErrorMessage('All fields are required.');
      return;
    }

    const success = db.addExecutive({
      executiveId: newId.trim().toUpperCase(),
      executiveName: newName.trim(),
      region: newRegion.trim()
    });

    if (!success) {
      setErrorMessage(`Executive ID ${newId.toUpperCase()} already exists.`);
      return;
    }

    setNewId('');
    setNewName('');
    setNewRegion('');
    setErrorMessage('');
    setIsAddModalOpen(false);
  };

  const handleToggleActive = (executiveId: string) => {
    db.toggleExecutiveStatus(executiveId);
  };

  return (
    <div className="space-y-6 max-w-6xl mx-auto">
      {/* Top Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-lg font-bold text-white tracking-tight flex items-center space-x-2">
            <Users className="w-5 h-5 text-purple-400" />
            <span>SUBAGENTS &amp; FIELD EXECUTIVES DIRECTORY</span>
          </h1>
          <p className="text-xs text-slate-400 mt-0.5">
            Authorized tollway subagents with assigned RFID custody registers.
          </p>
        </div>

        <div className="flex items-center space-x-2">
          <button
            onClick={() => setIsAddModalOpen(true)}
            className="px-4 py-2 bg-purple-600 hover:bg-purple-500 text-white rounded-xl text-xs font-bold flex items-center space-x-1.5 shadow-sm transition"
          >
            <Plus className="w-4 h-4" />
            <span>Add New Executive</span>
          </button>
        </div>
      </div>

      {/* Executives Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {execSummaries.map(exec => (
          <div
            key={exec.executiveId}
            className="bg-slate-800/80 border border-slate-700/70 rounded-2xl p-5 shadow-xs flex flex-col justify-between"
          >
            <div>
              <div className="flex items-start justify-between mb-3">
                <div>
                  <div className="flex items-center space-x-2">
                    <span className="font-mono text-xs font-bold px-2 py-0.5 rounded bg-purple-500/20 text-purple-300 border border-purple-500/30">
                      {exec.executiveId}
                    </span>
                    <h3 className="font-bold text-sm text-white">{exec.executiveName}</h3>
                  </div>
                  <div className="flex items-center text-xs text-slate-400 mt-1">
                    <MapPin className="w-3.5 h-3.5 mr-1 text-slate-500" />
                    <span>{exec.region}</span>
                  </div>
                </div>

                <button
                  onClick={() => handleToggleActive(exec.executiveId)}
                  className={`px-2 py-0.5 rounded-full text-[10px] font-bold border transition ${
                    exec.active
                      ? 'bg-emerald-500/15 text-emerald-300 border-emerald-500/30'
                      : 'bg-slate-700/60 text-slate-400 border-slate-600'
                  }`}
                >
                  {exec.active ? 'ACTIVE' : 'INACTIVE'}
                </button>
              </div>

              {/* Current Stock in hand */}
              <div className="p-3 bg-slate-900/60 rounded-xl border border-slate-700/50 mb-3">
                <div className="text-[11px] text-slate-400 mb-0.5">Physical Stock In Hand</div>
                <div className="flex items-baseline justify-between">
                  <span className={`font-mono text-xl font-bold ${exec.currentStock > 0 ? 'text-purple-300' : 'text-slate-500'}`}>
                    {exec.currentStock} tags
                  </span>
                  <span className="text-[10px] text-slate-500">Live RFID custody</span>
                </div>
              </div>
            </div>

            {/* Actions */}
            <div className="pt-3 border-t border-slate-700/50 flex items-center justify-between">
              <button
                onClick={() => onInspectExecutiveTags(exec.executiveId)}
                className="text-xs text-sky-400 hover:text-sky-300 font-semibold flex items-center space-x-1"
              >
                <Tag className="w-3.5 h-3.5" />
                <span>View Assigned Tags ({exec.currentStock})</span>
              </button>
            </div>
          </div>
        ))}
      </div>

      {/* Danger Zone: Database Reset */}
      <div className="p-5 bg-slate-900/60 border border-slate-800 rounded-2xl flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h4 className="text-xs font-bold text-slate-300 uppercase tracking-wider">Database State Management</h4>
          <p className="text-xs text-slate-400 mt-0.5">
            Reset database to baseline seeded state to re-run end-to-end acceptance tests.
          </p>
        </div>
        <div className="flex items-center space-x-2">
          <button
            onClick={() => {
              if (confirm('Are you sure you want to reset all inventory data? This will clear all tags and ledger entries.')) {
                db.resetDatabase();
              }
            }}
            className="px-3 py-1.5 bg-rose-500/10 hover:bg-rose-500/20 border border-rose-500/30 text-rose-300 rounded-lg text-xs font-semibold flex items-center space-x-1.5 transition"
          >
            <RefreshCw className="w-3.5 h-3.5" />
            <span>Reset Database</span>
          </button>
          <button
            onClick={onOpenAcceptanceTests}
            className="px-3.5 py-1.5 bg-emerald-600 hover:bg-emerald-500 text-white rounded-lg text-xs font-bold flex items-center space-x-1.5 shadow-xs transition"
          >
            <ShieldCheck className="w-3.5 h-3.5" />
            <span>Run Test Suite (A–G)</span>
          </button>
        </div>
      </div>

      {/* Add Executive Modal */}
      {isAddModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-xs p-4">
          <div className="bg-slate-800 border border-slate-700 rounded-2xl max-w-md w-full p-6 shadow-2xl">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-base font-bold text-white">Add New Field Subagent</h3>
              <button onClick={() => setIsAddModalOpen(false)} className="text-slate-400 hover:text-white">
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleAddExecutive} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">Executive ID</label>
                <input
                  type="text"
                  value={newId}
                  onChange={e => setNewId(e.target.value.toUpperCase())}
                  placeholder="e.g. EXEC-10"
                  className="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-xs font-mono text-white"
                  required
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">Full Name</label>
                <input
                  type="text"
                  value={newName}
                  onChange={e => setNewName(e.target.value)}
                  placeholder="e.g. Rajesh Mehra"
                  className="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white"
                  required
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">Region / Toll Plaza</label>
                <input
                  type="text"
                  value={newRegion}
                  onChange={e => setNewRegion(e.target.value)}
                  placeholder="e.g. Eastern Expressway KM 18"
                  className="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white"
                  required
                />
              </div>

              {errorMessage && (
                <div className="text-xs text-rose-400 flex items-center space-x-1">
                  <AlertTriangle className="w-3.5 h-3.5 shrink-0" />
                  <span>{errorMessage}</span>
                </div>
              )}

              <div className="flex justify-end space-x-2 pt-2">
                <button
                  type="button"
                  onClick={() => setIsAddModalOpen(false)}
                  className="px-4 py-2 bg-slate-700 text-slate-300 rounded-lg text-xs font-semibold"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="px-4 py-2 bg-purple-600 hover:bg-purple-500 text-white rounded-lg text-xs font-bold shadow-sm"
                >
                  Create Executive
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
