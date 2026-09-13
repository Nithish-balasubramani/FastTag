import React from 'react';
import { db } from '../data/db';
import { BarChart3, Printer, Download, Layers, ShieldCheck } from 'lucide-react';
import { LocationBadge } from '../components/LocationBadge';

export const ReportsScreen: React.FC = () => {
  const metrics = db.getOverallMetrics();
  const classSummaries = db.getClassStockSummaries();
  const execSummaries = db.getExecutiveStockSummaries();

  const handlePrint = () => {
    window.print();
  };

  return (
    <div className="space-y-6 max-w-6xl mx-auto">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-lg font-bold text-white tracking-tight flex items-center space-x-2">
            <BarChart3 className="w-5 h-5 text-sky-400" />
            <span>INVENTORY AUDIT &amp; STATUTORY REPORTS</span>
          </h1>
          <p className="text-xs text-slate-400 mt-0.5">
            All totals dynamically computed from individual RFID tag records in the immutable ledger.
          </p>
        </div>

        <div className="flex items-center space-x-2">
          <button
            onClick={handlePrint}
            className="px-3.5 py-1.5 bg-slate-800 hover:bg-slate-750 border border-slate-700 text-slate-300 rounded-xl text-xs font-semibold flex items-center space-x-1.5 transition"
          >
            <Printer className="w-4 h-4" />
            <span>Print Audit Report</span>
          </button>
        </div>
      </div>

      {/* Location Totals Card */}
      <div className="bg-slate-800/80 border border-slate-700/70 rounded-2xl p-6 shadow-xs">
        <h2 className="text-xs font-bold text-sky-400 uppercase tracking-wider mb-4 flex items-center space-x-2">
          <Layers className="w-4 h-4" />
          <span>Location-Wise Live Aggregations</span>
        </h2>

        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
          <div className="bg-slate-900/60 p-4 rounded-xl border border-slate-700/50">
            <div className="text-xs text-slate-400 mb-1">Central Warehouse</div>
            <div className="font-mono text-2xl font-bold text-amber-300">{metrics.centralStock}</div>
            <div className="mt-2">
              <LocationBadge location="CENTRAL" size="sm" />
            </div>
          </div>

          <div className="bg-slate-900/60 p-4 rounded-xl border border-slate-700/50">
            <div className="text-xs text-slate-400 mb-1">Master Inventory</div>
            <div className="font-mono text-2xl font-bold text-sky-300">{metrics.masterStock}</div>
            <div className="mt-2">
              <LocationBadge location="MASTER" size="sm" />
            </div>
          </div>

          <div className="bg-slate-900/60 p-4 rounded-xl border border-slate-700/50">
            <div className="text-xs text-slate-400 mb-1">Executive / Subagents</div>
            <div className="font-mono text-2xl font-bold text-purple-300">{metrics.executiveStock}</div>
            <div className="mt-2">
              <LocationBadge location="EXECUTIVE" size="sm" />
            </div>
          </div>

          <div className="bg-slate-900/60 p-4 rounded-xl border border-slate-700/50">
            <div className="text-xs text-slate-400 mb-1">Assigned / Installed</div>
            <div className="font-mono text-2xl font-bold text-emerald-300">{metrics.assignedStock}</div>
            <div className="mt-2">
              <LocationBadge location="ASSIGNED" size="sm" />
            </div>
          </div>
        </div>

        <div className="mt-4 pt-4 border-t border-slate-700/70 flex items-center justify-between">
          <span className="text-xs font-semibold text-slate-300">Total Active Tag Inventory in System:</span>
          <span className="font-mono text-lg font-bold text-white">{metrics.totalActive} units</span>
        </div>
      </div>

      {/* Class-wise Audit Matrix */}
      <div className="bg-slate-800/80 border border-slate-700/70 rounded-2xl p-6 shadow-xs">
        <h2 className="text-xs font-bold text-sky-400 uppercase tracking-wider mb-3">
          Class-Wise Inventory Audit Matrix
        </h2>

        {classSummaries.length === 0 ? (
          <div className="text-center py-8 text-xs text-slate-400">
            No tags found in inventory database.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead>
                <tr className="border-b border-slate-700 text-slate-400 font-semibold bg-slate-900/40">
                  <th className="p-3">Tag Class</th>
                  <th className="p-3">Central</th>
                  <th className="p-3">Master</th>
                  <th className="p-3">Executive</th>
                  <th className="p-3">Assigned</th>
                  <th className="p-3 text-right">Total Units</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800">
                {classSummaries.map(cls => (
                  <tr key={cls.tagClass} className="hover:bg-slate-750/30">
                    <td className="p-3 font-semibold text-slate-200">{cls.tagClass}</td>
                    <td className="p-3 font-mono text-amber-300">{cls.centralCount}</td>
                    <td className="p-3 font-mono font-bold text-sky-300">{cls.masterCount}</td>
                    <td className="p-3 font-mono text-purple-300">{cls.executiveCount}</td>
                    <td className="p-3 font-mono text-emerald-300">{cls.assignedCount}</td>
                    <td className="p-3 font-mono font-bold text-white text-right">{cls.totalCount}</td>
                  </tr>
                ))}
              </tbody>
              <tfoot>
                <tr className="border-t-2 border-slate-700 font-bold bg-slate-900/60">
                  <td className="p-3 text-white">Summary Totals</td>
                  <td className="p-3 font-mono text-amber-300">{metrics.centralStock}</td>
                  <td className="p-3 font-mono text-sky-300">{metrics.masterStock}</td>
                  <td className="p-3 font-mono text-purple-300">{metrics.executiveStock}</td>
                  <td className="p-3 font-mono text-emerald-300">{metrics.assignedStock}</td>
                  <td className="p-3 font-mono text-white text-right">{metrics.totalActive}</td>
                </tr>
              </tfoot>
            </table>
          </div>
        )}
      </div>

      {/* Executive Stock Breakdown */}
      <div className="bg-slate-800/80 border border-slate-700/70 rounded-2xl p-6 shadow-xs">
        <h2 className="text-xs font-bold text-purple-400 uppercase tracking-wider mb-3">
          Executive Stock Summary
        </h2>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead>
              <tr className="border-b border-slate-700 text-slate-400 font-semibold bg-slate-900/40">
                <th className="p-3">Executive</th>
                <th className="p-3">Region</th>
                <th className="p-3">Status</th>
                <th className="p-3 text-right">Stock in Hand</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800">
              {execSummaries.map(exec => (
                <tr key={exec.executiveId} className="hover:bg-slate-750/30">
                  <td className="p-3 font-semibold text-slate-200">
                    {exec.executiveName} <span className="text-slate-400 font-mono text-[11px]">({exec.executiveId})</span>
                  </td>
                  <td className="p-3 text-slate-400">{exec.region}</td>
                  <td className="p-3">
                    <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                      exec.active ? 'bg-emerald-500/15 text-emerald-300' : 'bg-slate-700 text-slate-400'
                    }`}>
                      {exec.active ? 'ACTIVE' : 'INACTIVE'}
                    </span>
                  </td>
                  <td className="p-3 text-right font-mono font-bold">
                    <span className={exec.currentStock > 0 ? 'text-purple-300' : 'text-slate-500'}>
                      {exec.currentStock} tags
                    </span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
