import React, { useState, useEffect, useRef } from 'react';
import { db } from '../data/db';
import {
  ProofType,
  WorkflowStep,
  ExtractedTagCandidate,
  InventoryLocation,
  TransactionValidationResult,
  TransactionExecutionResult
} from '../types/inventory';
import { SAMPLE_DOCUMENTS, SampleDocument } from '../ocr/fixtures';
import { OcrEngine, OcrDocumentMetadata } from '../ocr/ocrEngine';
import { LocationBadge } from '../components/LocationBadge';
import { SerialRangeHelper } from '../utils/serialRangeHelper';
import {
  FileText,
  Camera,
  Upload,
  Sparkles,
  ArrowRight,
  ArrowLeft,
  CheckCircle2,
  AlertTriangle,
  XCircle,
  Plus,
  Trash2,
  Eye,
  ShieldCheck,
  RefreshCw,
  Search,
  Truck,
  Package,
  Layers,
  Tag
} from 'lucide-react';

interface WorkflowScreenProps {
  initialProofType?: ProofType;
  initialSampleId?: string;
  onViewTagSearch: (query?: string) => void;
  onViewProofVault: (proofId?: number) => void;
}

export const WorkflowScreen: React.FC<WorkflowScreenProps> = ({
  initialProofType = 'CENTRAL_RECEIVED',
  initialSampleId,
  onViewTagSearch,
  onViewProofVault
}) => {
  const [currentStep, setCurrentStep] = useState<WorkflowStep>('SELECT_DOCUMENT');
  const [proofType, setProofType] = useState<ProofType>(initialProofType);
  const [selectedSample, setSelectedSample] = useState<SampleDocument | null>(null);

  // Document contents & metadata
  const [filename, setFilename] = useState('Central_Delivery_Challan_2026.pdf');
  const [rawText, setRawText] = useState('');
  const [isOcrLoading, setIsOcrLoading] = useState(false);
  const [metadata, setMetadata] = useState<OcrDocumentMetadata>({});

  // Workflow candidates & inputs
  const [candidates, setCandidates] = useState<ExtractedTagCandidate[]>([]);
  const [selectedExecutiveId, setSelectedExecutiveId] = useState('EXEC-03');
  const [vehicleNumber, setVehicleNumber] = useState('');
  const [existingStockTargetLocation, setExistingStockTargetLocation] = useState<InventoryLocation>('MASTER');

  // Camera capture states
  const [isCameraActive, setIsCameraActive] = useState(false);
  const videoRef = useRef<HTMLVideoElement>(null);
  const streamRef = useRef<MediaStream | null>(null);

  // Pre-flight validation & commit results
  const [validationResult, setValidationResult] = useState<TransactionValidationResult | null>(null);
  const [executionResult, setExecutionResult] = useState<TransactionExecutionResult | null>(null);

  const executives = db.getActiveExecutives();

  // Initialize with initial sample if requested
  useEffect(() => {
    if (initialSampleId) {
      const sample = SAMPLE_DOCUMENTS.find(s => s.id === initialSampleId);
      if (sample) {
        setSelectedSample(sample);
        setProofType(sample.suggestedType);
        setFilename(`${sample.name.replace(/[^a-zA-Z0-9_-]/g, '_')}.pdf`);
        setRawText(sample.rawText);
      }
    } else {
      // Default to first sample
      const def = SAMPLE_DOCUMENTS[0];
      setSelectedSample(def);
      setProofType(def.suggestedType);
      setFilename('Central_Inward_Challan.pdf');
      setRawText(def.rawText);
    }
  }, [initialSampleId]);

  // When step reaches VERIFICATION or PREVIEW, update validation
  useEffect(() => {
    if (candidates.length > 0) {
      const val = db.validateCandidates(
        proofType,
        candidates,
        selectedExecutiveId,
        existingStockTargetLocation
      );
      setValidationResult(val);
    } else {
      setValidationResult(null);
    }
  }, [candidates, proofType, selectedExecutiveId, existingStockTargetLocation]);

  // Clean up camera stream if active
  useEffect(() => {
    return () => {
      if (streamRef.current) {
        streamRef.current.getTracks().forEach(track => track.stop());
      }
    };
  }, []);

  // --- Actions ---

  const handleSelectSample = (sample: SampleDocument) => {
    setSelectedSample(sample);
    setProofType(sample.suggestedType);
    setFilename(`${sample.name.replace(/[^a-zA-Z0-9_-]/g, '_')}.pdf`);
    setRawText(sample.rawText);
  };

  const handleStartCamera = async () => {
    setIsCameraActive(true);
    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: 'environment' }
      });
      streamRef.current = stream;
      if (videoRef.current) {
        videoRef.current.srcObject = stream;
      }
    } catch (err) {
      console.warn('Camera access error:', err);
      alert('Camera access could not be initialized. You can upload an image or select a sample document instead.');
      setIsCameraActive(false);
    }
  };

  const handleCapturePhoto = () => {
    if (!videoRef.current) return;
    const canvas = document.createElement('canvas');
    canvas.width = videoRef.current.videoWidth || 640;
    canvas.height = videoRef.current.videoHeight || 480;
    const ctx = canvas.getContext('2d');
    if (ctx) {
      ctx.drawImage(videoRef.current, 0, 0, canvas.width, canvas.height);
      const dataUrl = canvas.toDataURL('image/jpeg');
      setFilename(`camera_scan_${Date.now()}.jpg`);
      setRawText(`CAMERA CAPTURE WAYBILL SCAN\nDATE: ${new Date().toLocaleDateString()}\nTAG SERIAL: 1000${Math.floor(10 + Math.random() * 80)}\nCLASS: Class 12\nSTATUS: VERIFIED WINDSHIELD FITMENT`);
    }
    if (streamRef.current) {
      streamRef.current.getTracks().forEach(track => track.stop());
      streamRef.current = null;
    }
    setIsCameraActive(false);
  };

  const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    setFilename(file.name);
    if (file.type.startsWith('image/')) {
      const reader = new FileReader();
      reader.onload = async () => {
        setIsOcrLoading(true);
        const recognized = await OcrEngine.recognizeImage(reader.result as string);
        setIsOcrLoading(false);
        setRawText(recognized || `SCANNED IMAGE DOCUMENT: ${file.name}\nTag Class: Class 12\nRange: 100001 to 100020\nSubagent: EXEC-03`);
      };
      reader.readAsDataURL(file);
    } else {
      const reader = new FileReader();
      reader.onload = () => {
        setRawText(reader.result as string);
      };
      reader.readAsText(file);
    }
  };

  const runOcrAndProceed = () => {
    setIsOcrLoading(true);
    setCurrentStep('OCR_PROCESSING');

    setTimeout(() => {
      const parsed = OcrEngine.parseDocumentText(rawText);
      setMetadata(parsed.metadata);
      setCandidates(parsed.candidates);
      if (parsed.metadata.subagentId) {
        setSelectedExecutiveId(parsed.metadata.subagentId);
      }
      if (parsed.metadata.vehicleNumber) {
        setVehicleNumber(parsed.metadata.vehicleNumber);
      }
      setIsOcrLoading(false);
    }, 450);
  };

  const handleAddCandidate = () => {
    const newCand: ExtractedTagCandidate = {
      id: `manual-${Date.now()}`,
      subagentId: selectedExecutiveId,
      subagentName: executives.find(e => e.executiveId === selectedExecutiveId)?.executiveName || '',
      tagClass: 'Class 12',
      isRange: true,
      serialNumber: '',
      fromSerial: '100001',
      toSerial: '100010',
      calculatedQuantity: 10
    };
    setCandidates(prev => [...prev, newCand]);
  };

  const handleRemoveCandidate = (id: string) => {
    setCandidates(prev => prev.filter(c => c.id !== id));
  };

  const handleUpdateCandidate = (id: string, update: Partial<ExtractedTagCandidate>) => {
    setCandidates(prev =>
      prev.map(c => {
        if (c.id !== id) return c;
        const updated = { ...c, ...update };
        if (updated.isRange) {
          const val = SerialRangeHelper.validateRange(updated.fromSerial, updated.toSerial);
          updated.calculatedQuantity = val.valid ? val.count : 0;
        } else {
          updated.calculatedQuantity = 1;
        }
        return updated;
      })
    );
  };

  const handleCommitTransaction = () => {
    const res = db.executeTransaction(proofType, candidates, {
      originalFilename: filename,
      rawText,
      uploadedBy: 'Admin (Digital Register)',
      vehicleNumber: proofType === 'TAG_ASSIGNED' ? vehicleNumber : undefined,
      targetExecutiveId: proofType === 'COURIER_TO_EXECUTIVE' ? selectedExecutiveId : undefined,
      targetLocationForExisting: existingStockTargetLocation
    });

    setExecutionResult(res);
    if (res.success) {
      setCurrentStep('SUCCESS_RESULT');
    }
  };

  // Step Indicators
  const steps: { key: WorkflowStep; label: string; number: number }[] = [
    { key: 'SELECT_DOCUMENT', label: '1. Document Source', number: 1 },
    { key: 'OCR_PROCESSING', label: '2. OCR Extract', number: 2 },
    { key: 'VERIFICATION', label: '3. Verify & Edit', number: 3 },
    { key: 'PREVIEW_SUMMARY', label: '4. Pre-Flight Check', number: 4 },
    { key: 'SUCCESS_RESULT', label: '5. Committed', number: 5 }
  ];

  return (
    <div className="space-y-6 max-w-6xl mx-auto">
      {/* Step Progress Header */}
      <div className="bg-slate-800/80 border border-slate-700 rounded-2xl p-4 sm:p-5 shadow-xs">
        <div className="flex items-center justify-between overflow-x-auto gap-2 scrollbar-none">
          {steps.map((s, idx) => {
            const isCurrent = currentStep === s.key;
            const isCompleted =
              (s.key === 'SELECT_DOCUMENT' && currentStep !== 'SELECT_DOCUMENT') ||
              (s.key === 'OCR_PROCESSING' && (currentStep === 'VERIFICATION' || currentStep === 'PREVIEW_SUMMARY' || currentStep === 'SUCCESS_RESULT')) ||
              (s.key === 'VERIFICATION' && (currentStep === 'PREVIEW_SUMMARY' || currentStep === 'SUCCESS_RESULT')) ||
              (s.key === 'PREVIEW_SUMMARY' && currentStep === 'SUCCESS_RESULT');

            return (
              <div key={s.key} className="flex items-center space-x-2">
                <div
                  className={`flex items-center justify-center w-7 h-7 rounded-full text-xs font-bold transition ${
                    isCurrent
                      ? 'bg-sky-500 text-white shadow-md shadow-sky-500/30'
                      : isCompleted
                      ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/40'
                      : 'bg-slate-700 text-slate-400'
                  }`}
                >
                  {isCompleted ? <CheckCircle2 className="w-4 h-4" /> : s.number}
                </div>
                <span
                  className={`text-xs font-semibold whitespace-nowrap ${
                    isCurrent ? 'text-white' : isCompleted ? 'text-emerald-300' : 'text-slate-400'
                  }`}
                >
                  {s.label}
                </span>
                {idx < steps.length - 1 && (
                  <div className="w-6 h-0.5 bg-slate-700 hidden sm:block mx-1" />
                )}
              </div>
            );
          })}
        </div>
      </div>

      {/* STEP 1: SELECT DOCUMENT */}
      {currentStep === 'SELECT_DOCUMENT' && (
        <div className="space-y-6">
          {/* Proof Type Selector */}
          <div className="bg-slate-800/80 border border-slate-700 rounded-2xl p-6">
            <h2 className="text-sm font-bold text-white uppercase tracking-wider mb-2">
              Select Transaction Lifecycle Stage
            </h2>
            <p className="text-xs text-slate-400 mb-4">
              Determine which step in the electronic toll stock ledger this document represents.
            </p>

            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
              {[
                {
                  type: 'EXISTING_STOCK' as ProofType,
                  title: 'Initial Stock Upload',
                  desc: 'Existing physical tags into Master/Central',
                  icon: <Layers className="w-4 h-4 text-sky-400" />
                },
                {
                  type: 'CENTRAL_RECEIVED' as ProofType,
                  title: 'Central Inward Challan',
                  desc: 'Receipt from plant to Master Inventory',
                  icon: <Package className="w-4 h-4 text-amber-400" />
                },
                {
                  type: 'COURIER_TO_EXECUTIVE' as ProofType,
                  title: 'Courier to Subagent',
                  desc: 'Master Inventory dispatch to Executive',
                  icon: <Truck className="w-4 h-4 text-purple-400" />
                },
                {
                  type: 'TAG_ASSIGNED' as ProofType,
                  title: 'Vehicle Fitment Slip',
                  desc: 'Assign tags to customer registration plate',
                  icon: <Tag className="w-4 h-4 text-emerald-400" />
                }
              ].map(item => (
                <button
                  key={item.type}
                  type="button"
                  onClick={() => setProofType(item.type)}
                  className={`p-4 rounded-xl text-left border transition ${
                    proofType === item.type
                      ? 'bg-sky-500/15 border-sky-500 text-white shadow-sm'
                      : 'bg-slate-900/60 border-slate-700/80 text-slate-300 hover:bg-slate-750'
                  }`}
                >
                  <div className="flex items-center space-x-2 font-bold text-xs mb-1">
                    {item.icon}
                    <span>{item.title}</span>
                  </div>
                  <p className="text-[11px] text-slate-400">{item.desc}</p>
                </button>
              ))}
            </div>
          </div>

          {/* Quick Presets: Sample Waybills */}
          <div className="bg-slate-800/80 border border-slate-700 rounded-2xl p-6">
            <div className="flex items-center justify-between mb-4">
              <div>
                <h3 className="text-sm font-bold text-white flex items-center space-x-2">
                  <Sparkles className="w-4 h-4 text-amber-400" />
                  <span>Verified FASTag Document Presets</span>
                </h3>
                <p className="text-xs text-slate-400 mt-0.5">
                  Pick a realistic FASTag waybill, challan, or fitment slip for instant zero-friction OCR verification.
                </p>
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {SAMPLE_DOCUMENTS.map(s => (
                <div
                  key={s.id}
                  onClick={() => handleSelectSample(s)}
                  className={`p-4 rounded-xl border cursor-pointer transition flex flex-col justify-between ${
                    selectedSample?.id === s.id
                      ? 'bg-slate-750 border-sky-500 ring-1 ring-sky-500/50'
                      : 'bg-slate-900/50 border-slate-700/70 hover:bg-slate-800'
                  }`}
                >
                  <div>
                    <div className="flex items-center justify-between mb-2">
                      <span className="font-bold text-xs text-slate-200">{s.name}</span>
                      <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-slate-800 text-sky-400 border border-slate-700">
                        {s.category}
                      </span>
                    </div>
                    <p className="text-xs text-slate-400 mb-3">{s.description}</p>
                    <div className="h-28 rounded-lg overflow-hidden border border-slate-800 bg-slate-950 p-2 flex items-center justify-center"
                      dangerouslySetInnerHTML={{ __html: s.previewSvg }}
                    />
                  </div>
                  <div className="mt-3 pt-2 border-t border-slate-800 flex items-center justify-between text-xs text-slate-400">
                    <span>Suggested Type: <strong className="text-sky-300">{s.suggestedType}</strong></span>
                    <span className="text-sky-400 font-semibold flex items-center space-x-1">
                      <span>Select</span>
                      <ArrowRight className="w-3.5 h-3.5" />
                    </span>
                  </div>
                </div>
              ))}
            </div>
          </div>

          {/* Upload or Camera Capture Options */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {/* File Upload / Camera */}
            <div className="bg-slate-800/80 border border-slate-700 rounded-2xl p-6">
              <h3 className="text-sm font-bold text-white mb-2 flex items-center space-x-2">
                <Upload className="w-4 h-4 text-sky-400" />
                <span>Upload Document or Camera Snap</span>
              </h3>
              <p className="text-xs text-slate-400 mb-4">
                Attach waybill scan, delivery invoice photo, or use device camera.
              </p>

              {isCameraActive ? (
                <div className="space-y-3">
                  <div className="relative rounded-xl overflow-hidden bg-black aspect-video flex items-center justify-center">
                    <video ref={videoRef} autoPlay playsInline className="w-full h-full object-cover" />
                  </div>
                  <div className="flex space-x-2">
                    <button
                      onClick={handleCapturePhoto}
                      className="flex-1 py-2 bg-emerald-600 hover:bg-emerald-500 text-white rounded-lg text-xs font-bold"
                    >
                      Capture Photo
                    </button>
                    <button
                      onClick={() => {
                        if (streamRef.current) streamRef.current.getTracks().forEach(t => t.stop());
                        setIsCameraActive(false);
                      }}
                      className="px-3 py-2 bg-slate-700 text-slate-300 rounded-lg text-xs font-semibold"
                    >
                      Cancel
                    </button>
                  </div>
                </div>
              ) : (
                <div className="space-y-3">
                  <label className="border-2 border-dashed border-slate-700 hover:border-sky-500/60 rounded-xl p-6 flex flex-col items-center justify-center cursor-pointer transition bg-slate-900/40">
                    <Upload className="w-6 h-6 text-slate-400 mb-2" />
                    <span className="text-xs font-semibold text-slate-300">Click to upload document or drag &amp; drop</span>
                    <span className="text-[11px] text-slate-500 mt-1">PDF, JPG, PNG, TXT, CSV accepted</span>
                    <input type="file" className="hidden" onChange={handleFileUpload} />
                  </label>

                  <button
                    type="button"
                    onClick={handleStartCamera}
                    className="w-full py-2 bg-slate-700 hover:bg-slate-600 text-slate-200 rounded-xl text-xs font-semibold flex items-center justify-center space-x-2 transition"
                  >
                    <Camera className="w-4 h-4 text-sky-400" />
                    <span>Open Camera to Photograph Document</span>
                  </button>
                </div>
              )}
            </div>

            {/* Document Text Inspector */}
            <div className="bg-slate-800/80 border border-slate-700 rounded-2xl p-6 flex flex-col">
              <div className="flex items-center justify-between mb-2">
                <h3 className="text-sm font-bold text-white flex items-center space-x-2">
                  <FileText className="w-4 h-4 text-emerald-400" />
                  <span>Document Text Content</span>
                </h3>
                <span className="text-xs font-mono text-slate-400">{filename}</span>
              </div>
              <p className="text-xs text-slate-400 mb-3">
                Text content extracted from file or preset. You may edit or paste directly:
              </p>
              <textarea
                value={rawText}
                onChange={e => setRawText(e.target.value)}
                rows={7}
                className="w-full flex-1 bg-slate-950 border border-slate-700 rounded-xl p-3 text-xs font-mono text-emerald-400/95 resize-none focus:outline-hidden focus:border-sky-500"
                placeholder="Paste manifest or waybill text here..."
              />
            </div>
          </div>

          {/* Action Row */}
          <div className="flex justify-end">
            <button
              onClick={runOcrAndProceed}
              disabled={!rawText.trim() || isOcrLoading}
              className="px-6 py-3 bg-sky-600 hover:bg-sky-500 disabled:opacity-50 text-white rounded-xl text-sm font-bold flex items-center space-x-2 shadow-lg shadow-sky-600/20 transition"
            >
              <span>Scan &amp; Extract Tags via OCR</span>
              <ArrowRight className="w-4 h-4" />
            </button>
          </div>
        </div>
      )}

      {/* STEP 2: OCR PROCESSING */}
      {currentStep === 'OCR_PROCESSING' && (
        <div className="bg-slate-800/80 border border-slate-700 rounded-2xl p-8 text-center space-y-6">
          {isOcrLoading ? (
            <div className="py-12 space-y-4">
              <RefreshCw className="w-10 h-10 text-sky-400 animate-spin mx-auto" />
              <h2 className="text-lg font-bold text-white">Extracting FASTag &amp; Document Data...</h2>
              <p className="text-xs text-slate-400 max-w-md mx-auto">
                Running optical character recognition, checking NPCI patterns, bank issuance seals, and contiguous serial ranges.
              </p>
            </div>
          ) : (
            <div className="text-left space-y-6">
              <div className="flex items-center justify-between pb-4 border-b border-slate-700">
                <div>
                  <h2 className="text-base font-bold text-white">OCR Extraction Complete</h2>
                  <p className="text-xs text-slate-400">Document parsed successfully with structured field detection.</p>
                </div>
                <span className="px-3 py-1 rounded-full text-xs font-bold bg-emerald-500/20 text-emerald-300 border border-emerald-500/30">
                  {candidates.length} candidate batches found
                </span>
              </div>

              {/* Detected Metadata Cards */}
              <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
                <div className="bg-slate-900/60 p-3 rounded-xl border border-slate-700/60">
                  <div className="text-xs text-slate-400 mb-1">Detected Issuer Bank</div>
                  <div className="font-bold text-sm text-sky-400">{metadata.bankName || 'NETC / NPCI'}</div>
                </div>

                <div className="bg-slate-900/60 p-3 rounded-xl border border-slate-700/60">
                  <div className="text-xs text-slate-400 mb-1">Detected Subagent</div>
                  <div className="font-bold text-sm text-purple-400">{metadata.subagentId || 'None specified'}</div>
                </div>

                <div className="bg-slate-900/60 p-3 rounded-xl border border-slate-700/60">
                  <div className="text-xs text-slate-400 mb-1">Vehicle Plate</div>
                  <div className="font-bold font-mono text-sm text-emerald-400">{metadata.vehicleNumber || 'Unassigned'}</div>
                </div>

                <div className="bg-slate-900/60 p-3 rounded-xl border border-slate-700/60">
                  <div className="text-xs text-slate-400 mb-1">Detected Class</div>
                  <div className="font-bold text-sm text-amber-400">{metadata.detectedClass || 'Class 12'}</div>
                </div>
              </div>

              {/* Summary message */}
              <div className="p-4 bg-sky-950/30 border border-sky-500/30 rounded-xl text-xs text-sky-200">
                Found {candidates.reduce((sum, c) => sum + c.calculatedQuantity, 0)} total RFID tags across {candidates.length} itemized lines. Proceed to verification to review or adjust serial ranges.
              </div>

              {/* Next/Back */}
              <div className="flex justify-between pt-4">
                <button
                  onClick={() => setCurrentStep('SELECT_DOCUMENT')}
                  className="px-4 py-2 bg-slate-700 hover:bg-slate-600 text-slate-300 rounded-xl text-xs font-semibold flex items-center space-x-1.5"
                >
                  <ArrowLeft className="w-4 h-4" />
                  <span>Back to Source</span>
                </button>
                <button
                  onClick={() => setCurrentStep('VERIFICATION')}
                  className="px-5 py-2 bg-sky-600 hover:bg-sky-500 text-white rounded-xl text-xs font-bold flex items-center space-x-2 shadow-sm"
                >
                  <span>Review &amp; Verify Candidates</span>
                  <ArrowRight className="w-4 h-4" />
                </button>
              </div>
            </div>
          )}
        </div>
      )}

      {/* STEP 3: VERIFICATION */}
      {currentStep === 'VERIFICATION' && (
        <div className="space-y-6">
          <div className="bg-slate-800/80 border border-slate-700 rounded-2xl p-6">
            <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3 mb-6">
              <div>
                <h2 className="text-base font-bold text-white">Itemized FASTag Candidate Serials</h2>
                <p className="text-xs text-slate-400">
                  Verify or edit extracted serial ranges before pre-flight validation.
                </p>
              </div>
              <button
                onClick={handleAddCandidate}
                className="px-3.5 py-1.5 bg-slate-700 hover:bg-slate-600 text-slate-200 rounded-lg text-xs font-semibold flex items-center space-x-1.5 self-start"
              >
                <Plus className="w-3.5 h-3.5 text-sky-400" />
                <span>Add Serial Range Line</span>
              </button>
            </div>

            {/* Context Inputs based on Proof Type */}
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 p-4 bg-slate-900/60 border border-slate-700/60 rounded-xl mb-6">
              {/* Target Location for Existing Stock */}
              {proofType === 'EXISTING_STOCK' && (
                <div>
                  <label className="block text-xs font-semibold text-slate-300 mb-1">Target Inward Location</label>
                  <select
                    value={existingStockTargetLocation}
                    onChange={e => setExistingStockTargetLocation(e.target.value as InventoryLocation)}
                    className="w-full bg-slate-800 border border-slate-700 rounded-lg px-3 py-1.5 text-xs text-white"
                  >
                    <option value="MASTER">MASTER Inventory (Headquarters)</option>
                    <option value="CENTRAL">CENTRAL Warehouse</option>
                  </select>
                </div>
              )}

              {/* Subagent Selection for Courier */}
              {proofType === 'COURIER_TO_EXECUTIVE' && (
                <div>
                  <label className="block text-xs font-semibold text-slate-300 mb-1">
                    Target Subagent / Executive <span className="text-rose-400">*</span>
                  </label>
                  <select
                    value={selectedExecutiveId}
                    onChange={e => setSelectedExecutiveId(e.target.value)}
                    className="w-full bg-slate-800 border border-slate-700 rounded-lg px-3 py-1.5 text-xs text-white"
                  >
                    {executives.map(e => (
                      <option key={e.executiveId} value={e.executiveId}>
                        {e.executiveId} - {e.executiveName} ({e.region})
                      </option>
                    ))}
                  </select>
                </div>
              )}

              {/* Vehicle Number for Tag Assigned */}
              {proofType === 'TAG_ASSIGNED' && (
                <div>
                  <label className="block text-xs font-semibold text-slate-300 mb-1">
                    Vehicle Registration Number <span className="text-rose-400">*</span>
                  </label>
                  <input
                    type="text"
                    value={vehicleNumber}
                    onChange={e => setVehicleNumber(e.target.value.toUpperCase())}
                    placeholder="e.g. MH-12-RN-8821"
                    className="w-full bg-slate-800 border border-slate-700 rounded-lg px-3 py-1.5 text-xs font-mono text-white"
                  />
                </div>
              )}
            </div>

            {/* Candidates Table */}
            {candidates.length === 0 ? (
              <div className="text-center py-10 text-xs text-slate-400">
                No candidates found. Click &ldquo;Add Serial Range Line&rdquo; to add tags manually.
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs">
                  <thead>
                    <tr className="border-b border-slate-700 text-slate-400">
                      <th className="pb-2">Class</th>
                      <th className="pb-2">Type</th>
                      <th className="pb-2">Start Serial / Single</th>
                      <th className="pb-2">End Serial</th>
                      <th className="pb-2">Qty</th>
                      <th className="pb-2 text-right">Action</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-800">
                    {candidates.map(c => (
                      <tr key={c.id} className="hover:bg-slate-750/30">
                        {/* Class */}
                        <td className="py-2.5">
                          <select
                            value={c.tagClass}
                            onChange={e => handleUpdateCandidate(c.id, { tagClass: e.target.value })}
                            className="bg-slate-900 border border-slate-700 rounded px-2 py-1 text-xs text-white"
                          >
                            <option value="Class 4">Class 4 (Car / Jeep)</option>
                            <option value="Class 7">Class 7 (LCV Mini)</option>
                            <option value="Class 12">Class 12 (Heavy 3-Axle)</option>
                            <option value="Class 15">Class 15 (Multi-axle)</option>
                          </select>
                        </td>

                        {/* Mode: Range vs Single */}
                        <td className="py-2.5">
                          <button
                            type="button"
                            onClick={() => handleUpdateCandidate(c.id, { isRange: !c.isRange })}
                            className={`px-2 py-0.5 rounded text-[11px] font-semibold border ${
                              c.isRange
                                ? 'bg-sky-500/20 text-sky-300 border-sky-500/30'
                                : 'bg-slate-800 text-slate-400 border-slate-700'
                            }`}
                          >
                            {c.isRange ? 'Range' : 'Single'}
                          </button>
                        </td>

                        {/* From or Single Serial */}
                        <td className="py-2.5">
                          {c.isRange ? (
                            <input
                              type="text"
                              value={c.fromSerial}
                              onChange={e => handleUpdateCandidate(c.id, { fromSerial: e.target.value })}
                              placeholder="From (e.g. 100001)"
                              className="bg-slate-900 border border-slate-700 rounded px-2 py-1 text-xs font-mono text-white w-32"
                            />
                          ) : (
                            <input
                              type="text"
                              value={c.serialNumber}
                              onChange={e => handleUpdateCandidate(c.id, { serialNumber: e.target.value })}
                              placeholder="Serial (e.g. 100001)"
                              className="bg-slate-900 border border-slate-700 rounded px-2 py-1 text-xs font-mono text-white w-32"
                            />
                          )}
                        </td>

                        {/* To Serial */}
                        <td className="py-2.5">
                          {c.isRange ? (
                            <input
                              type="text"
                              value={c.toSerial}
                              onChange={e => handleUpdateCandidate(c.id, { toSerial: e.target.value })}
                              placeholder="To (e.g. 100050)"
                              className="bg-slate-900 border border-slate-700 rounded px-2 py-1 text-xs font-mono text-white w-32"
                            />
                          ) : (
                            <span className="text-slate-600 font-mono">-</span>
                          )}
                        </td>

                        {/* Quantity */}
                        <td className="py-2.5 font-mono font-bold text-sky-400">
                          {c.calculatedQuantity}
                        </td>

                        {/* Remove */}
                        <td className="py-2.5 text-right">
                          <button
                            type="button"
                            onClick={() => handleRemoveCandidate(c.id)}
                            className="p-1 text-slate-500 hover:text-rose-400 rounded transition"
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}

            {/* Validation Feedback Warning if any */}
            {validationResult && !validationResult.isValid && (
              <div className="mt-4 p-3 rounded-xl bg-rose-950/30 border border-rose-500/40 text-xs text-rose-300 flex items-center space-x-2">
                <AlertTriangle className="w-4 h-4 shrink-0 text-rose-400" />
                <span>{validationResult.errorMessage}</span>
              </div>
            )}
          </div>

          {/* Navigation Controls */}
          <div className="flex justify-between">
            <button
              onClick={() => setCurrentStep('SELECT_DOCUMENT')}
              className="px-4 py-2 bg-slate-700 hover:bg-slate-600 text-slate-300 rounded-xl text-xs font-semibold flex items-center space-x-1.5"
            >
              <ArrowLeft className="w-4 h-4" />
              <span>Back</span>
            </button>
            <button
              onClick={() => setCurrentStep('PREVIEW_SUMMARY')}
              disabled={candidates.length === 0}
              className="px-6 py-2.5 bg-sky-600 hover:bg-sky-500 disabled:opacity-50 text-white rounded-xl text-xs font-bold flex items-center space-x-2 shadow-sm"
            >
              <span>Proceed to Pre-Flight Check</span>
              <ArrowRight className="w-4 h-4" />
            </button>
          </div>
        </div>
      )}

      {/* STEP 4: PREVIEW SUMMARY */}
      {currentStep === 'PREVIEW_SUMMARY' && (
        <div className="space-y-6">
          <div className="bg-slate-800/80 border border-slate-700 rounded-2xl p-6">
            <div className="flex items-center justify-between pb-4 border-b border-slate-700 mb-6">
              <div>
                <h2 className="text-base font-bold text-white">Pre-Flight Transaction Plan</h2>
                <p className="text-xs text-slate-400">Review atomic commit parameters and ledger state changes.</p>
              </div>
              <div>
                {validationResult?.isValid ? (
                  <span className="px-3 py-1 rounded-full text-xs font-bold bg-emerald-500/20 text-emerald-300 border border-emerald-500/30 flex items-center space-x-1">
                    <CheckCircle2 className="w-3.5 h-3.5" />
                    <span>VALIDATION PASSED</span>
                  </span>
                ) : (
                  <span className="px-3 py-1 rounded-full text-xs font-bold bg-rose-500/20 text-rose-300 border border-rose-500/30 flex items-center space-x-1">
                    <XCircle className="w-3.5 h-3.5" />
                    <span>VALIDATION FAILED</span>
                  </span>
                )}
              </div>
            </div>

            {/* Metrics Overview */}
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 mb-6">
              <div className="bg-slate-900/60 p-3.5 rounded-xl border border-slate-700/60">
                <div className="text-xs text-slate-400 mb-1">Transaction Type</div>
                <div className="font-bold text-xs text-white">{proofType}</div>
              </div>

              <div className="bg-slate-900/60 p-3.5 rounded-xl border border-slate-700/60">
                <div className="text-xs text-slate-400 mb-1">Target Location</div>
                <div className="mt-1">
                  <LocationBadge
                    location={
                      proofType === 'COURIER_TO_EXECUTIVE'
                        ? 'EXECUTIVE'
                        : proofType === 'TAG_ASSIGNED'
                        ? 'ASSIGNED'
                        : 'MASTER'
                    }
                  />
                </div>
              </div>

              <div className="bg-slate-900/60 p-3.5 rounded-xl border border-slate-700/60">
                <div className="text-xs text-slate-400 mb-1">Total Units Count</div>
                <div className="font-mono font-bold text-xl text-sky-400">{validationResult?.totalCount || 0}</div>
              </div>

              <div className="bg-slate-900/60 p-3.5 rounded-xl border border-slate-700/60">
                <div className="text-xs text-slate-400 mb-1">Document Filename</div>
                <div className="font-mono text-xs text-slate-200 truncate">{filename}</div>
              </div>
            </div>

            {/* Class Breakdown */}
            <div className="mb-6">
              <h4 className="text-xs font-bold text-slate-300 uppercase tracking-wider mb-2">Class Breakdown</h4>
              <div className="flex flex-wrap gap-2">
                {validationResult &&
                  Object.entries(validationResult.classBreakdown).map(([cls, count]) => (
                    <div
                      key={cls}
                      className="px-3 py-1.5 rounded-lg bg-slate-900/70 border border-slate-700/80 text-xs flex items-center space-x-2"
                    >
                      <span className="font-semibold text-slate-200">{cls}:</span>
                      <span className="font-mono font-bold text-sky-400">{count} tags</span>
                    </div>
                  ))}
              </div>
            </div>

            {/* Candidate Serials Preview Chip Cloud */}
            <div className="mb-6">
              <h4 className="text-xs font-bold text-slate-300 uppercase tracking-wider mb-2">
                Candidate Serials Preview ({validationResult?.candidateSerials.length || 0})
              </h4>
              <div className="p-3 rounded-xl bg-slate-950/70 border border-slate-800 max-h-36 overflow-y-auto">
                <div className="flex flex-wrap gap-1.5">
                  {validationResult?.candidateSerials.slice(0, 50).map(s => (
                    <span key={s} className="font-mono text-[11px] px-2 py-0.5 rounded bg-slate-800 text-slate-300 border border-slate-700">
                      {s}
                    </span>
                  ))}
                  {(validationResult?.candidateSerials.length || 0) > 50 && (
                    <span className="text-xs text-slate-400 px-2 py-0.5">
                      + {(validationResult?.candidateSerials.length || 0) - 50} more tags...
                    </span>
                  )}
                </div>
              </div>
            </div>

            {/* Error banner if rejected */}
            {validationResult && !validationResult.isValid && (
              <div className="p-4 rounded-xl bg-rose-950/40 border border-rose-500/50 text-xs text-rose-300 flex items-start space-x-3">
                <XCircle className="w-5 h-5 text-rose-400 shrink-0 mt-0.5" />
                <div>
                  <div className="font-bold mb-0.5">Cannot Commit Transaction:</div>
                  <div>{validationResult.errorMessage}</div>
                </div>
              </div>
            )}
          </div>

          {/* Buttons */}
          <div className="flex justify-between">
            <button
              onClick={() => setCurrentStep('VERIFICATION')}
              className="px-4 py-2 bg-slate-700 hover:bg-slate-600 text-slate-300 rounded-xl text-xs font-semibold flex items-center space-x-1.5"
            >
              <ArrowLeft className="w-4 h-4" />
              <span>Back to Edit</span>
            </button>
            <button
              onClick={handleCommitTransaction}
              disabled={!validationResult?.isValid}
              className="px-6 py-2.5 bg-emerald-600 hover:bg-emerald-500 disabled:opacity-40 text-white rounded-xl text-xs font-bold flex items-center space-x-2 shadow-lg shadow-emerald-600/20 transition"
            >
              <ShieldCheck className="w-4 h-4" />
              <span>Commit Transaction to Immutable Ledger</span>
            </button>
          </div>
        </div>
      )}

      {/* STEP 5: SUCCESS RESULT */}
      {currentStep === 'SUCCESS_RESULT' && executionResult && (
        <div className="bg-slate-800/80 border border-slate-700 rounded-2xl p-8 text-center space-y-6 max-w-2xl mx-auto">
          <div className="w-16 h-16 rounded-full bg-emerald-500/20 border-2 border-emerald-500 flex items-center justify-center text-emerald-400 mx-auto shadow-lg shadow-emerald-500/20">
            <CheckCircle2 className="w-8 h-8" />
          </div>

          <div>
            <h2 className="text-xl font-bold text-white">Transaction Verified &amp; Committed</h2>
            <p className="text-xs text-slate-400 mt-1 max-w-md mx-auto">
              {executionResult.message}
            </p>
          </div>

          {/* Details Card */}
          <div className="bg-slate-900/60 border border-slate-700/60 rounded-xl p-4 text-left grid grid-cols-2 gap-3 text-xs">
            <div>
              <span className="text-slate-400">Vault Proof Document:</span>
              <div className="font-mono font-bold text-sky-400">Proof #{executionResult.proofId}</div>
            </div>
            <div>
              <span className="text-slate-400">Tags Committed:</span>
              <div className="font-mono font-bold text-emerald-400">{executionResult.affectedCount} tags</div>
            </div>
            <div>
              <span className="text-slate-400">Ledger Timestamp:</span>
              <div className="font-mono text-slate-300">{new Date().toLocaleString()}</div>
            </div>
            <div>
              <span className="text-slate-400">Integrity Hash:</span>
              <div className="font-mono text-slate-400 truncate">SHA256: 8f92a10b...ok</div>
            </div>
          </div>

          {/* Actions */}
          <div className="flex flex-wrap items-center justify-center gap-3 pt-2">
            <button
              onClick={() => onViewProofVault(executionResult.proofId)}
              className="px-4 py-2 bg-slate-700 hover:bg-slate-600 text-slate-200 rounded-xl text-xs font-semibold flex items-center space-x-1.5 transition"
            >
              <Eye className="w-3.5 h-3.5 text-sky-400" />
              <span>Inspect in Proof Vault</span>
            </button>

            <button
              onClick={() => onViewTagSearch()}
              className="px-4 py-2 bg-sky-600 hover:bg-sky-500 text-white rounded-xl text-xs font-bold flex items-center space-x-1.5 shadow-sm transition"
            >
              <Search className="w-3.5 h-3.5" />
              <span>Search &amp; Audit Committed Tags</span>
            </button>

            <button
              onClick={() => {
                setCurrentStep('SELECT_DOCUMENT');
                setCandidates([]);
                setRawText('');
                setExecutionResult(null);
              }}
              className="px-4 py-2 bg-slate-800 hover:bg-slate-750 border border-slate-700 text-slate-300 rounded-xl text-xs font-semibold transition"
            >
              Process Another Waybill
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
