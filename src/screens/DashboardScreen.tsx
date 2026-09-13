import React from 'react';
import { db } from '../data/db';
import { AppScreen, ProofType, MovementWithProof } from '../types/inventory';
import { LocationBadge } from '../components/LocationBadge';
import {
  Package,
  Layers,
  Users,
  CheckCircle2,
  TrendingUp,
  ArrowRight,
  ShieldCheck,
  PlusCircle,
  Truck,
  FileCheck2,
  Tag
} from 'lucide-react';

interface DashboardScreenProps {
  onNavigate: (screen: AppScreen) => void;
  onStartWorkflow: (proofType: ProofType, sampleId?: string) => void;
  onInspectTag: (serial: string) => void;
  onInspectProof: (proofId: number) => void;
}

export const DashboardScreen: React.FC<DashboardScreenProps> = ({
  onNavigate,
  onStartWorkflow,
  onInspectTag,
  onInspectProof
}) => {
  const metrics = db.getOverallMetrics();
  const classSummaries = db.getClassStockSummaries();
  const execSummaries = db.getExecutiveStockSummaries();
  const recentMovements = db.getRecentMovementsWithProof(10);

  return (
    <div className="space-y-6">
      {/* Top Banner / Welcome */}
      <div className="bg-gradient-to-r from-slate-900 via-slate-800 to-slate-900 border border-slate-700/80 rounded-2xl p-6 shadow-sm">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div>
            <div className="flex items-center space-x-2">
              <h1 className="text-xl sm:text-2xl font-black text-white tracking-tight">
                FASTag Stock Register & Audit Ledger
              </h1>
              <span className="px-2 py-0.5 rounded text-[11px] font-bold bg-emerald-500/20 text-emerald-400 border border-emerald-500/30">
                ACTIVE
              </span>
            </div>
            <p className="text-xs sm:text-sm text-slate-400 mt-1">
              Multi-tier inventory tracking across Central Warehouse, Master Inventory, Field Executives, and Vehicle Fitments.
            </p>
          </div>

          <div className="flex items-center space-x-2">
            <button
              onClick={() => onStartWorkflow('CENTRAL_RECEIVED', 'sample-central-inward')}
              className="px-3.5 py-2 bg-sky-600 hover:bg-sky-500 text-white rounded-xl text-xs font-bold flex items-center space-x-1.5 shadow-sm transition"
            >
              <FileCheck2 className="w-4 h-4" />
              <span>Inward Delivery Challan</span>
            </button>
            <button
              onClick={() => onNavigate('WORKFLOW')}
              className="px-3.5 py-2 bg-slate-700 hover:bg-slate-600 text-slate-200 rounded-xl text-xs font-semibold flex items-center space-x-1.5 transition"
            >
              <span>Custom Workflow</span>
              <ArrowRight className="w-3.5 h-3.5" />
            </button>
          </div>
        </div>
      </div>

      {/* 4 Location Metric Cards */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Central */}
        <div className="bg-slate-800/80 border border-slate-700/70 rounded-xl p-4 relative overflow-hidden">
          <div className="flex items-center justify-between text-xs text-amber-400 font-semibold mb-2">
            <span>Central Warehouse</span>
            <div className="p-1.5 rounded-lg bg-amber-500/10 text-amber-400">
              <Package className="w-4 h-4" />
            </div>
          </div>
          <div className="text-2xl sm:text-3xl font-mono font-bold text-white">{metrics.centralStock}</div>
          <div className="text-[11px] text-slate-400 mt-1 flex items-center justify-between">
            <span>Production Hub Stock</span>
            <LocationBadge location="CENTRAL" size="sm" />
          </div>
        </div>

        {/* Master */}
        <div className="bg-slate-800/80 border border-slate-700/70 rounded-xl p-4 relative overflow-hidden">
          <div className="flex items-center justify-between text-xs text-sky-400 font-semibold mb-2">
            <span>Master Inventory</span>
            <div className="p-1.5 rounded-lg bg-sky-500/10 text-sky-400">
              <Layers className="w-4 h-4" />
            </div>
          </div>
          <div className="text-2xl sm:text-3xl font-mono font-bold text-white">{metrics.masterStock}</div>
          <div className="text-[11px] text-slate-400 mt-1 flex items-center justify-between">
            <span>Headquarters Vault</span>
            <LocationBadge location="MASTER" size="sm" />
          </div>
        </div>

        {/* Executive */}
        <div className="bg-slate-800/80 border border-slate-700/70 rounded-xl p-4 relative overflow-hidden">
          <div className="flex items-center justify-between text-xs text-purple-400 font-semibold mb-2">
            <span>Executive / Subagents</span>
            <div className="p-1.5 rounded-lg bg-purple-500/10 text-purple-400">
              <Users className="w-4 h-4" />
            </div>
          </div>
          <div className="text-2xl sm:text-3xl font-mono font-bold text-white">{metrics.executiveStock}</div>
          <div className="text-[11px] text-slate-400 mt-1 flex items-center justify-between">
            <span>Dispatched Field Stock</span>
            <LocationBadge location="EXECUTIVE" size="sm" />
          </div>
        </div>

        {/* Assigned */}
        <div className="bg-slate-800/80 border border-slate-700/70 rounded-xl p-4 relative overflow-hidden">
          <div className="flex items-center justify-between text-xs text-emerald-400 font-semibold mb-2">
            <span>Assigned / Installed</span>
            <div className="p-1.5 rounded-lg bg-emerald-500/10 text-emerald-400">
              <CheckCircle2 className="w-4 h-4" />
            </div>
          </div>
          <div className="text-2xl sm:text-3xl font-mono font-bold text-white">{metrics.assignedStock}</div>
          <div className="text-[11px] text-slate-400 mt-1 flex items-center justify-between">
            <span>Vehicle Toll Fitments</span>
            <LocationBadge location="ASSIGNED" size="sm" />
          </div>
        </div>
      </div>

      {/* Quick Actions Panel */}
      <div className="bg-slate-800/60 border border-slate-700/60 rounded-xl p-5">
        <h2 className="text-xs font-bold text-slate-300 uppercase tracking-wider mb-3">
          Process Transactions &amp; Documents
        </h2>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
          {/* Action 1: Upload Existing Stock */}
          <button
            onClick={() => onStartWorkflow('EXISTING_STOCK')}
            className="text-left p-3.5 bg-slate-850 hover:bg-slate-750 border border-slate-700 hover:border-sky-500/50 rounded-xl transition group"
          >
            <div className="flex items-center space-x-2 text-sky-400 text-xs font-bold mb-1">
              <PlusCircle className="w-4 h-4 group-hover:scale-110 transition" />
              <span>Initial Stock Inward</span>
            </div>
            <p className="text-xs text-slate-400">Upload existing physical warehouse baseline tags into Master Inventory.</p>
          </button>

          {/* Action 2: Receive Central Stock */}
          <button
            onClick={() => onStartWorkflow('CENTRAL_RECEIVED', 'sample-central-inward')}
            className="text-left p-3.5 bg-slate-850 hover:bg-slate-750 border border-slate-700 hover:border-amber-500/50 rounded-xl transition group"
          >
            <div className="flex items-center space-x-2 text-amber-400 text-xs font-bold mb-1">
              <Package className="w-4 h-4 group-hover:scale-110 transition" />
              <span>Central Receipt Challan</span>
            </div>
            <p className="text-xs text-slate-400">Process manufacturing delivery challan into Master Inventory via OCR.</p>
          </button>

          {/* Action 3: Courier Dispatch */}
          <button
            onClick={() => onStartWorkflow('COURIER_TO_EXECUTIVE', 'sample-courier-dispatch')}
            className="text-left p-3.5 bg-slate-850 hover:bg-slate-750 border border-slate-700 hover:border-purple-500/50 rounded-xl transition group"
          >
            <div className="flex items-center space-x-2 text-purple-400 text-xs font-bold mb-1">
              <Truck className="w-4 h-4 group-hover:scale-110 transition" />
              <span>Courier to Subagent</span>
            </div>
            <p className="text-xs text-slate-400">Transfer tags from Master Inventory to Field Executive with courier manifest.</p>
          </button>

          {/* Action 4: Vehicle Fitment */}
          <button
            onClick={() => onStartWorkflow('TAG_ASSIGNED', 'sample-tag-assigned')}
            className="text-left p-3.5 bg-slate-850 hover:bg-slate-750 border border-slate-700 hover:border-emerald-500/50 rounded-xl transition group"
          >
            <div className="flex items-center space-x-2 text-emerald-400 text-xs font-bold mb-1">
              <Tag className="w-4 h-4 group-hover:scale-110 transition" />
              <span>Customer Fitment Slip</span>
            </div>
            <p className="text-xs text-slate-400">Assign executive tags to customer vehicle registration plate.</p>
          </button>
        </div>
      </div>

      {/* 2-Column: Class Breakdown & Executive Stock */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Class Breakdown */}
        <div className="bg-slate-800/70 border border-slate-700/60 rounded-xl p-5">
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center space-x-2">
              <TrendingUp className="w-4 h-4 text-sky-400" />
              <h3 className="text-sm font-bold text-white">Class-wise Inventory Breakdown</h3>
            </div>
            <button
              onClick={() => onNavigate('REPORTS')}
              className="text-xs text-sky-400 hover:text-sky-300 font-semibold"
            >
              Full Audit Matrix &rarr;
            </button>
          </div>

          {classSummaries.length === 0 ? (
            <div className="text-center py-8 text-xs text-slate-400">
              No inventory tags registered yet. Run inward workflow or acceptance test suite.
            </div>
          ) : (
            <div className="space-y-3">
              {classSummaries.map(cls => (
                <div key={cls.tagClass} className="bg-slate-900/60 border border-slate-700/50 rounded-lg p-3">
                  <div className="flex items-center justify-between text-xs mb-2">
                    <span className="font-bold text-slate-200">{cls.tagClass}</span>
                    <span className="font-mono font-bold text-sky-400">{cls.totalCount} total units</span>
                  </div>
                  <div className="grid grid-cols-4 gap-1 text-[11px] text-center">
                    <div className="bg-slate-800 rounded py-1">
                      <div className="text-slate-400">Central</div>
                      <div className="font-mono font-bold text-amber-300">{cls.centralCount}</div>
                    </div>
                    <div className="bg-slate-800 rounded py-1">
                      <div className="text-slate-400">Master</div>
                      <div className="font-mono font-bold text-sky-300">{cls.masterCount}</div>
                    </div>
                    <div className="bg-slate-800 rounded py-1">
                      <div className="text-slate-400">Exec</div>
                      <div className="font-mono font-bold text-purple-300">{cls.executiveCount}</div>
                    </div>
                    <div className="bg-slate-800 rounded py-1">
                      <div className="text-slate-400">Assigned</div>
                      <div className="font-mono font-bold text-emerald-300">{cls.assignedCount}</div>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>

        {/* Executive Stock Overview */}
        <div className="bg-slate-800/70 border border-slate-700/60 rounded-xl p-5">
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center space-x-2">
              <Users className="w-4 h-4 text-purple-400" />
              <h3 className="text-sm font-bold text-white">Subagents &amp; Field Executives</h3>
            </div>
            <button
              onClick={() => onNavigate('EXECUTIVES')}
              className="text-xs text-purple-400 hover:text-purple-300 font-semibold"
            >
              Manage Executives &rarr;
            </button>
          </div>

          <div className="space-y-2 max-h-[300px] overflow-y-auto pr-1">
            {execSummaries.slice(0, 6).map(exec => (
              <div
                key={exec.executiveId}
                className="flex items-center justify-between p-2.5 bg-slate-900/50 hover:bg-slate-900/80 border border-slate-700/40 rounded-lg transition"
              >
                <div>
                  <div className="text-xs font-semibold text-slate-200">
                    {exec.executiveName} <span className="text-slate-400 font-mono">({exec.executiveId})</span>
                  </div>
                  <div className="text-[11px] text-slate-400">{exec.region}</div>
                </div>
                <div className="flex items-center space-x-2">
                  <span className={`px-2 py-0.5 rounded text-xs font-mono font-bold ${
                    exec.currentStock > 0
                      ? 'bg-purple-500/20 text-purple-300 border border-purple-500/30'
                      : 'bg-slate-800 text-slate-500'
                  }`}>
                    {exec.currentStock} tags
                  </span>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Recent Immutable Stock Movement Ledger */}
      <div className="bg-slate-800/70 border border-slate-700/60 rounded-xl p-5">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center space-x-2">
            <ShieldCheck className="w-4 h-4 text-emerald-400" />
            <h3 className="text-sm font-bold text-white">Recent Immutable Stock Movement Ledger</h3>
          </div>
          <span className="text-xs text-slate-400 font-mono">Last 10 entries</span>
        </div>

        {recentMovements.length === 0 ? (
          <div className="text-center py-8 text-xs text-slate-400">
            No stock movement ledger records found. Execute a transaction or run the acceptance test suite.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead>
                <tr className="border-b border-slate-700 text-slate-400 font-semibold">
                  <th className="pb-2.5 font-mono">Serial Number</th>
                  <th className="pb-2.5">Class</th>
                  <th className="pb-2.5">Transition Path</th>
                  <th className="pb-2.5">Subagent</th>
                  <th className="pb-2.5">Proof Document</th>
                  <th className="pb-2.5 text-right">Timestamp</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800">
                {recentMovements.map(m => (
                  <tr key={m.id} className="hover:bg-slate-750/50 transition">
                    <td className="py-2.5 font-mono font-semibold text-sky-400">
                      <button
                        onClick={() => onInspectTag(m.tagSerialNumber)}
                        className="hover:underline text-left"
                      >
                        {m.tagSerialNumber}
                      </button>
                    </td>
                    <td className="py-2.5 text-slate-300">{m.tagClass}</td>
                    <td className="py-2.5">
                      <div className="flex items-center space-x-1.5">
                        <LocationBadge location={m.previousLocation || 'CENTRAL'} size="sm" />
                        <span className="text-slate-500">&rarr;</span>
                        <LocationBadge location={m.newLocation} size="sm" />
                      </div>
                    </td>
                    <td className="py-2.5 text-slate-300">
                      {m.subagentName} <span className="text-slate-500 font-mono text-[10px]">({m.subagentId})</span>
                    </td>
                    <td className="py-2.5">
                      <button
                        onClick={() => onInspectProof(m.proofDocumentId)}
                        className="inline-flex items-center space-x-1 text-sky-400 hover:text-sky-300 font-mono underline"
                      >
                        <ShieldCheck className="w-3.5 h-3.5" />
                        <span>#{m.proofDocumentId}</span>
                      </button>
                    </td>
                    <td className="py-2.5 text-right font-mono text-slate-400 text-[11px]">
                      {new Date(m.createdAt).toLocaleTimeString()}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};
