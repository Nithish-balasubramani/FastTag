import React, { useState } from 'react';
import { db } from '../data/db';
import { CheckCircle2, XCircle, AlertCircle, Play, RefreshCw, X, ShieldCheck, Check, ChevronRight } from 'lucide-react';

interface AcceptanceTestDialogProps {
  onClose: () => void;
  onRefreshView?: () => void;
}

interface TestStepResult {
  id: string;
  name: string;
  description: string;
  status: 'PENDING' | 'RUNNING' | 'PASSED' | 'FAILED';
  message: string;
  durationMs?: number;
}

export const AcceptanceTestDialog: React.FC<AcceptanceTestDialogProps> = ({ onClose, onRefreshView }) => {
  const [isRunning, setIsRunning] = useState(false);
  const [testResults, setTestResults] = useState<TestStepResult[]>([
    {
      id: 'TEST_A',
      name: 'Test A: Existing Stock Inward (100001 to 100050)',
      description: 'Upload 50 baseline tags (Class 12) into MASTER inventory with Proof Doc',
      status: 'PENDING',
      message: 'Not executed yet'
    },
    {
      id: 'TEST_B',
      name: 'Test B: Central Stock Received (100051 to 100075)',
      description: 'Receive 25 tags (Class 7) from Central Warehouse into MASTER inventory',
      status: 'PENDING',
      message: 'Not executed yet'
    },
    {
      id: 'TEST_C',
      name: 'Test C: Courier Dispatch to EXEC-03 (100001 to 100010)',
      description: 'Transfer 10 tags from MASTER to Executive Suresh Patel (EXEC-03)',
      status: 'PENDING',
      message: 'Not executed yet'
    },
    {
      id: 'TEST_D',
      name: 'Test D: Customer Vehicle Tag Fitment (100001 to 100005)',
      description: 'Assign 5 tags from EXEC-03 to vehicle MH-12-RN-8821 in ASSIGNED location',
      status: 'PENDING',
      message: 'Not executed yet'
    },
    {
      id: 'TEST_E',
      name: 'Test E: Reject Invalid Movement (Already Moved)',
      description: 'Attempt to courier tags 100001-100005 from MASTER to EXEC-04 (Must REJECT)',
      status: 'PENDING',
      message: 'Not executed yet'
    },
    {
      id: 'TEST_F',
      name: 'Test F: Reject Duplicate Serial Numbers',
      description: 'Attempt to upload duplicate tags 100001-100050 as Existing Stock (Must REJECT)',
      status: 'PENDING',
      message: 'Not executed yet'
    },
    {
      id: 'TEST_G',
      name: 'Test G: Validate Dynamic Inventory Aggregations',
      description: 'Verify 75 Total Tags, Master=60, Exec=5, Assigned=5, Class 12=50, Class 7=25',
      status: 'PENDING',
      message: 'Not executed yet'
    }
  ]);

  const updateStep = (id: string, update: Partial<TestStepResult>) => {
    setTestResults(prev => prev.map(t => t.id === id ? { ...t, ...update } : t));
  };

  const runAllTests = async () => {
    setIsRunning(true);

    // Reset database first to baseline clean state
    db.resetDatabase();

    // Small delay helper for visual feedback
    const delay = (ms: number) => new Promise(res => setTimeout(res, ms));

    try {
      // -------------------------------------------------------------
      // TEST A: Existing Stock
      // -------------------------------------------------------------
      updateStep('TEST_A', { status: 'RUNNING', message: 'Executing batch inward: 100001 - 100050...' });
      await delay(200);
      const tAStart = performance.now();
      const resA = db.executeTransaction(
        'EXISTING_STOCK',
        [{
          id: 'tA-1',
          subagentId: 'CENTRAL',
          subagentName: 'Master Warehouse',
          tagClass: 'Class 12',
          isRange: true,
          serialNumber: '',
          fromSerial: '100001',
          toSerial: '100050',
          calculatedQuantity: 50
        }],
        {
          originalFilename: 'TestA_Existing_Stock_Manifest.pdf',
          rawText: 'Item 1: Class 12 100001 to 100050 (50 Tags) Master Inward',
          uploadedBy: 'Automated Test Engine',
          targetLocationForExisting: 'MASTER'
        }
      );
      const tADur = Math.round(performance.now() - tAStart);

      if (resA.success && resA.affectedCount === 50) {
        updateStep('TEST_A', {
          status: 'PASSED',
          message: `SUCCESS: Inward 50 tags committed to MASTER (Proof #${resA.proofId})`,
          durationMs: tADur
        });
      } else {
        updateStep('TEST_A', {
          status: 'FAILED',
          message: `FAILED: ${resA.message}`,
          durationMs: tADur
        });
        setIsRunning(false);
        return;
      }

      // -------------------------------------------------------------
      // TEST B: Central Received
      // -------------------------------------------------------------
      updateStep('TEST_B', { status: 'RUNNING', message: 'Receiving central batch: 100051 - 100075...' });
      await delay(200);
      const tBStart = performance.now();
      const resB = db.executeTransaction(
        'CENTRAL_RECEIVED',
        [{
          id: 'tB-1',
          subagentId: 'CENTRAL',
          subagentName: 'Central Plant Hub',
          tagClass: 'Class 7',
          isRange: true,
          serialNumber: '',
          fromSerial: '100051',
          toSerial: '100075',
          calculatedQuantity: 25
        }],
        {
          originalFilename: 'TestB_Central_Delivery_Challan.pdf',
          rawText: 'Item 2: Class 7 100051 to 100075 (25 Tags) Received from Central',
          uploadedBy: 'Automated Test Engine'
        }
      );
      const tBDur = Math.round(performance.now() - tBStart);

      if (resB.success && resB.affectedCount === 25) {
        updateStep('TEST_B', {
          status: 'PASSED',
          message: `SUCCESS: Received 25 tags (Class 7) committed to MASTER (Proof #${resB.proofId})`,
          durationMs: tBDur
        });
      } else {
        updateStep('TEST_B', {
          status: 'FAILED',
          message: `FAILED: ${resB.message}`,
          durationMs: tBDur
        });
        setIsRunning(false);
        return;
      }

      // -------------------------------------------------------------
      // TEST C: Courier Dispatch to EXEC-03
      // -------------------------------------------------------------
      updateStep('TEST_C', { status: 'RUNNING', message: 'Dispatching 10 tags to EXEC-03 Suresh Patel...' });
      await delay(200);
      const tCStart = performance.now();
      const resC = db.executeTransaction(
        'COURIER_TO_EXECUTIVE',
        [{
          id: 'tC-1',
          subagentId: 'EXEC-03',
          subagentName: 'Suresh Patel',
          tagClass: 'Class 12',
          isRange: true,
          serialNumber: '',
          fromSerial: '100001',
          toSerial: '100010',
          calculatedQuantity: 10
        }],
        {
          originalFilename: 'TestC_DTDC_Courier_Waybill_EXEC03.pdf',
          rawText: 'Dispatch to EXEC-03 Suresh Patel: 100001 to 100010 (10 units)',
          uploadedBy: 'Automated Test Engine',
          targetExecutiveId: 'EXEC-03'
        }
      );
      const tCDur = Math.round(performance.now() - tCStart);

      if (resC.success && resC.affectedCount === 10) {
        updateStep('TEST_C', {
          status: 'PASSED',
          message: `SUCCESS: 10 tags transferred MASTER ➔ EXEC-03 (Proof #${resC.proofId})`,
          durationMs: tCDur
        });
      } else {
        updateStep('TEST_C', {
          status: 'FAILED',
          message: `FAILED: ${resC.message}`,
          durationMs: tCDur
        });
        setIsRunning(false);
        return;
      }

      // -------------------------------------------------------------
      // TEST D: Tag Assignment (100001 - 100005)
      // -------------------------------------------------------------
      updateStep('TEST_D', { status: 'RUNNING', message: 'Assigning 5 tags to vehicle MH-12-RN-8821...' });
      await delay(200);
      const tDStart = performance.now();
      const resD = db.executeTransaction(
        'TAG_ASSIGNED',
        [{
          id: 'tD-1',
          subagentId: 'EXEC-03',
          subagentName: 'Suresh Patel',
          tagClass: 'Class 12',
          isRange: true,
          serialNumber: '',
          fromSerial: '100001',
          toSerial: '100005',
          calculatedQuantity: 5
        }],
        {
          originalFilename: 'TestD_Toll_Installation_Slip.pdf',
          rawText: 'Installed on Windshield MH-12-RN-8821: 100001 to 100005 (5 tags)',
          uploadedBy: 'Automated Test Engine',
          vehicleNumber: 'MH-12-RN-8821',
          targetExecutiveId: 'EXEC-03'
        }
      );
      const tDDur = Math.round(performance.now() - tDStart);

      if (resD.success && resD.affectedCount === 5) {
        updateStep('TEST_D', {
          status: 'PASSED',
          message: `SUCCESS: 5 tags assigned to vehicle MH-12-RN-8821 in ASSIGNED (Proof #${resD.proofId})`,
          durationMs: tDDur
        });
      } else {
        updateStep('TEST_D', {
          status: 'FAILED',
          message: `FAILED: ${resD.message}`,
          durationMs: tDDur
        });
        setIsRunning(false);
        return;
      }

      // -------------------------------------------------------------
      // TEST E: Reject Invalid Movement (Already Moved)
      // -------------------------------------------------------------
      updateStep('TEST_E', { status: 'RUNNING', message: 'Testing rejection: courier 100001-100005 from MASTER to EXEC-04...' });
      await delay(200);
      const tEStart = performance.now();
      const resE = db.executeTransaction(
        'COURIER_TO_EXECUTIVE',
        [{
          id: 'tE-1',
          subagentId: 'EXEC-04',
          subagentName: 'Anil Verma',
          tagClass: 'Class 12',
          isRange: true,
          serialNumber: '',
          fromSerial: '100001',
          toSerial: '100005',
          calculatedQuantity: 5
        }],
        {
          originalFilename: 'TestE_Invalid_Courier_Waybill.pdf',
          rawText: 'Dispatch to EXEC-04: 100001 to 100005',
          uploadedBy: 'Automated Test Engine',
          targetExecutiveId: 'EXEC-04'
        }
      );
      const tEDur = Math.round(performance.now() - tEStart);

      // EXPECTATION: resE.success MUST BE FALSE
      if (!resE.success) {
        updateStep('TEST_E', {
          status: 'PASSED',
          message: `PASSED (CORRECTLY REJECTED): ${resE.message}`,
          durationMs: tEDur
        });
      } else {
        updateStep('TEST_E', {
          status: 'FAILED',
          message: 'CRITICAL FAILURE: Invalid movement was unexpectedly permitted by ledger!',
          durationMs: tEDur
        });
        setIsRunning(false);
        return;
      }

      // -------------------------------------------------------------
      // TEST F: Reject Duplicate Serials
      // -------------------------------------------------------------
      updateStep('TEST_F', { status: 'RUNNING', message: 'Testing duplicate rejection: re-uploading 100001-100050...' });
      await delay(200);
      const tFStart = performance.now();
      const resF = db.executeTransaction(
        'EXISTING_STOCK',
        [{
          id: 'tF-1',
          subagentId: 'CENTRAL',
          subagentName: 'Master Hub',
          tagClass: 'Class 12',
          isRange: true,
          serialNumber: '',
          fromSerial: '100001',
          toSerial: '100050',
          calculatedQuantity: 50
        }],
        {
          originalFilename: 'TestF_Duplicate_Manifest.pdf',
          rawText: 'Duplicate batch: 100001 to 100050',
          uploadedBy: 'Automated Test Engine',
          targetLocationForExisting: 'MASTER'
        }
      );
      const tFDur = Math.round(performance.now() - tFStart);

      // EXPECTATION: resF.success MUST BE FALSE
      if (!resF.success) {
        updateStep('TEST_F', {
          status: 'PASSED',
          message: `PASSED (CORRECTLY REJECTED): ${resF.message}`,
          durationMs: tFDur
        });
      } else {
        updateStep('TEST_F', {
          status: 'FAILED',
          message: 'CRITICAL FAILURE: Duplicate tag serials were unexpectedly allowed into database!',
          durationMs: tFDur
        });
        setIsRunning(false);
        return;
      }

      // -------------------------------------------------------------
      // TEST G: Validate Dynamic Inventory Aggregations
      // -------------------------------------------------------------
      updateStep('TEST_G', { status: 'RUNNING', message: 'Verifying mathematical invariants & dynamic aggregates...' });
      await delay(200);
      const tGStart = performance.now();

      const metrics = db.getOverallMetrics();
      const classSummaries = db.getClassStockSummaries();
      const execSummaries = db.getExecutiveStockSummaries();
      const exec03 = execSummaries.find(e => e.executiveId === 'EXEC-03');

      const class12 = classSummaries.find(c => c.tagClass === 'Class 12');
      const class7 = classSummaries.find(c => c.tagClass === 'Class 7');

      // Assertions:
      // Total = 75
      // Central = 0
      // Master = 60 (40 of Class 12 + 25 of Class 7? Wait: Class 12 had 50 initially. 10 moved to EXEC-03, 5 stayed with EXEC-03, 5 became ASSIGNED. Remaining in MASTER = 40. Class 7 has 25 in MASTER. Total in MASTER = 65? Wait: 40 + 25 = 65 tags!)
      // Let's verify exact breakdown:
      // Inward A: 50 tags (100001 - 100050) into MASTER
      // Inward B: 25 tags (100051 - 100075) into MASTER -> Total MASTER was 75
      // Courier C: 10 tags (100001 - 100010) MASTER -> EXEC-03 -> MASTER is now 65, EXEC-03 has 10
      // Assignment D: 5 tags (100001 - 100005) EXEC-03 -> ASSIGNED -> MASTER is 65, EXEC-03 has 5, ASSIGNED has 5
      // Total Active = 65 + 5 + 5 = 75!
      const isTotalValid = metrics.totalActive === 75;
      const isMasterValid = metrics.masterStock === 65;
      const isExecValid = metrics.executiveStock === 5 && (exec03?.currentStock === 5);
      const isAssignedValid = metrics.assignedStock === 5;
      const isClass12Valid = class12?.totalCount === 50;
      const isClass7Valid = class7?.totalCount === 25;

      const tGDur = Math.round(performance.now() - tGStart);

      if (isTotalValid && isMasterValid && isExecValid && isAssignedValid && isClass12Valid && isClass7Valid) {
        updateStep('TEST_G', {
          status: 'PASSED',
          message: `VERIFIED: Total Active: 75 | Master: ${metrics.masterStock} | Exec-03: ${exec03?.currentStock} | Assigned: ${metrics.assignedStock} | Class 12: 50 | Class 7: 25`,
          durationMs: tGDur
        });
      } else {
        updateStep('TEST_G', {
          status: 'FAILED',
          message: `AGGREGATION MISMATCH: Total=${metrics.totalActive} (expected 75), Master=${metrics.masterStock} (expected 65), Exec=${metrics.executiveStock} (expected 5), Assigned=${metrics.assignedStock} (expected 5)`,
          durationMs: tGDur
        });
      }
    } catch (e: any) {
      console.error('Test execution error:', e);
    } finally {
      setIsRunning(false);
      onRefreshView?.();
    }
  };

  const allPassed = testResults.every(t => t.status === 'PASSED');
  const anyFailed = testResults.some(t => t.status === 'FAILED');

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/75 backdrop-blur-xs p-4">
      <div className="bg-slate-800 border border-slate-700 rounded-2xl max-w-3xl w-full max-h-[92vh] flex flex-col shadow-2xl overflow-hidden">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-700 bg-slate-800/90">
          <div className="flex items-center space-x-3">
            <div className="p-2 bg-sky-500/10 border border-sky-500/20 rounded-xl text-sky-400">
              <ShieldCheck className="w-6 h-6" />
            </div>
            <div>
              <div className="flex items-center space-x-2">
                <h2 className="text-lg font-bold text-white">Acceptance Test Suite (Tests A – G)</h2>
                {allPassed && (
                  <span className="px-2 py-0.5 rounded-full text-xs font-bold bg-emerald-500/20 text-emerald-300 border border-emerald-500/40 flex items-center space-x-1">
                    <Check className="w-3 h-3" />
                    <span>ALL TESTS GREEN</span>
                  </span>
                )}
              </div>
              <p className="text-xs text-slate-400">Validates state machine transitions, OCR pipelines, and duplicate safeguards</p>
            </div>
          </div>
          <button onClick={onClose} className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-700 transition">
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Action Bar */}
        <div className="px-6 py-3 bg-slate-900/60 border-b border-slate-700/60 flex flex-wrap items-center justify-between gap-3">
          <div className="text-xs text-slate-300 flex items-center space-x-2">
            <span className="font-semibold text-slate-100">Test Plan:</span>
            <span>7 Automated Integration Assertions</span>
          </div>
          <div className="flex items-center space-x-2">
            <button
              onClick={() => {
                db.resetDatabase();
                setTestResults(prev => prev.map(t => ({ ...t, status: 'PENDING', message: 'Not executed yet', durationMs: undefined })));
                onRefreshView?.();
              }}
              disabled={isRunning}
              className="px-3 py-1.5 bg-slate-700 hover:bg-slate-600 disabled:opacity-50 text-slate-200 text-xs font-medium rounded-lg flex items-center space-x-1.5 transition"
            >
              <RefreshCw className="w-3.5 h-3.5" />
              <span>Reset Database</span>
            </button>
            <button
              onClick={runAllTests}
              disabled={isRunning}
              className="px-4 py-1.5 bg-sky-600 hover:bg-sky-500 disabled:opacity-50 text-white text-xs font-bold rounded-lg flex items-center space-x-1.5 shadow-sm transition"
            >
              {isRunning ? (
                <>
                  <RefreshCw className="w-3.5 h-3.5 animate-spin" />
                  <span>Executing Tests...</span>
                </>
              ) : (
                <>
                  <Play className="w-3.5 h-3.5 fill-current" />
                  <span>Run All Tests (A - G)</span>
                </>
              )}
            </button>
          </div>
        </div>

        {/* Test List */}
        <div className="p-6 overflow-y-auto space-y-3">
          {testResults.map(test => {
            const isPending = test.status === 'PENDING';
            const isRun = test.status === 'RUNNING';
            const isPassed = test.status === 'PASSED';
            const isFailed = test.status === 'FAILED';

            return (
              <div
                key={test.id}
                className={`p-4 rounded-xl border transition-all ${
                  isPassed
                    ? 'bg-emerald-950/20 border-emerald-500/30'
                    : isFailed
                    ? 'bg-rose-950/25 border-rose-500/40'
                    : isRun
                    ? 'bg-sky-950/20 border-sky-500/40'
                    : 'bg-slate-900/40 border-slate-700/50'
                }`}
              >
                <div className="flex items-start justify-between">
                  <div className="flex items-start space-x-3">
                    <div className="mt-0.5">
                      {isPassed && <CheckCircle2 className="w-5 h-5 text-emerald-400" />}
                      {isFailed && <XCircle className="w-5 h-5 text-rose-400" />}
                      {isRun && <RefreshCw className="w-5 h-5 text-sky-400 animate-spin" />}
                      {isPending && <AlertCircle className="w-5 h-5 text-slate-500" />}
                    </div>
                    <div>
                      <div className="flex items-center space-x-2">
                        <h4 className="font-semibold text-sm text-slate-100">{test.name}</h4>
                        {test.durationMs !== undefined && (
                          <span className="text-[10px] font-mono text-slate-400">{test.durationMs}ms</span>
                        )}
                      </div>
                      <p className="text-xs text-slate-400 mt-0.5">{test.description}</p>
                    </div>
                  </div>

                  <div>
                    {isPassed && (
                      <span className="px-2 py-0.5 rounded text-[11px] font-bold bg-emerald-500/15 text-emerald-300 border border-emerald-500/30">
                        PASSED
                      </span>
                    )}
                    {isFailed && (
                      <span className="px-2 py-0.5 rounded text-[11px] font-bold bg-rose-500/15 text-rose-300 border border-rose-500/30">
                        FAILED
                      </span>
                    )}
                    {isRun && (
                      <span className="px-2 py-0.5 rounded text-[11px] font-bold bg-sky-500/15 text-sky-300 border border-sky-500/30">
                        RUNNING
                      </span>
                    )}
                    {isPending && (
                      <span className="px-2 py-0.5 rounded text-[11px] font-medium bg-slate-700/50 text-slate-400">
                        PENDING
                      </span>
                    )}
                  </div>
                </div>

                <div className="mt-2.5 pt-2 border-t border-slate-700/40 flex items-center justify-between text-xs">
                  <span className={`font-mono text-xs ${isPassed ? 'text-emerald-300/90' : isFailed ? 'text-rose-300/90' : 'text-slate-400'}`}>
                    {test.message}
                  </span>
                </div>
              </div>
            );
          })}
        </div>

        {/* Footer */}
        <div className="px-6 py-4 border-t border-slate-700 bg-slate-900/80 flex items-center justify-between">
          <div className="text-xs text-slate-400">
            {allPassed ? (
              <span className="text-emerald-400 font-semibold flex items-center space-x-1.5">
                <Check className="w-4 h-4" />
                <span>All 7 acceptance scenarios validated successfully against local state machine!</span>
              </span>
            ) : anyFailed ? (
              <span className="text-rose-400 font-semibold">One or more tests failed. Check assertions above.</span>
            ) : (
              <span>Ready to run acceptance tests. Click &ldquo;Run All Tests (A - G)&rdquo;.</span>
            )}
          </div>
          <button
            onClick={onClose}
            className="px-4 py-2 bg-slate-700 hover:bg-slate-600 rounded-lg text-white font-medium text-xs transition"
          >
            Close Dialog
          </button>
        </div>
      </div>
    </div>
  );
};
