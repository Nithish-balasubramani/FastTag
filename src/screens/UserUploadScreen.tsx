import React, { useState, useRef, useEffect } from 'react';
import { db } from '../data/db';
import { LoggedInUser } from '../types/inventory';
import { Camera, Upload, CheckCircle2, AlertCircle, RefreshCw, Tag, ShieldCheck, Truck, Sparkles } from 'lucide-react';
import { OcrEngine } from '../ocr/ocrEngine';

interface UserUploadScreenProps {
  currentUser: LoggedInUser;
  onTagCommitted?: (serial: string) => void;
}

export const UserUploadScreen: React.FC<UserUploadScreenProps> = ({ currentUser, onTagCommitted }) => {
  const [tagSerial, setTagSerial] = useState('100001');
  const [vehicleNumber, setVehicleNumber] = useState('MH-12-RN-8821');
  const [tagClass, setTagClass] = useState('Class 12');
  const [isCameraActive, setIsCameraActive] = useState(false);
  const [isScanning, setIsScanning] = useState(false);
  const [photoDataUrl, setPhotoDataUrl] = useState<string | null>(null);
  const [submitSuccess, setSubmitSuccess] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const videoRef = useRef<HTMLVideoElement>(null);
  const streamRef = useRef<MediaStream | null>(null);

  useEffect(() => {
    return () => {
      if (streamRef.current) {
        streamRef.current.getTracks().forEach(t => t.stop());
      }
    };
  }, []);

  const handleStartCamera = async () => {
    setIsCameraActive(true);
    setErrorMessage(null);
    try {
      const stream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: 'environment' }
      });
      streamRef.current = stream;
      if (videoRef.current) {
        videoRef.current.srcObject = stream;
      }
    } catch (err) {
      console.warn('Camera error:', err);
      setErrorMessage('Camera access unavailable. You may upload a photo or use the sample preset below.');
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
      setPhotoDataUrl(dataUrl);

      // Trigger instant simulated OCR on snapshot
      setIsScanning(true);
      setTimeout(() => {
        setIsScanning(false);
      }, 500);
    }

    if (streamRef.current) {
      streamRef.current.getTracks().forEach(t => t.stop());
      streamRef.current = null;
    }
    setIsCameraActive(false);
  };

  const handleLoadSamplePhoto = () => {
    setTagSerial('100001');
    setVehicleNumber('MH-12-RN-8821');
    setTagClass('Class 12');
    setPhotoDataUrl('data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" width="300" height="180" viewBox="0 0 300 180"><rect width="300" height="180" fill="%230f172a"/><rect x="20" y="20" width="260" height="140" rx="8" fill="%231e293b" stroke="%2338bdf8" stroke-width="2"/><text x="35" y="55" fill="%2338bdf8" font-family="monospace" font-size="14" font-weight="bold">NETC FASTAG</text><text x="35" y="85" fill="%23ffffff" font-family="monospace" font-size="16" font-weight="bold">100001</text><text x="35" y="115" fill="%2310b981" font-family="monospace" font-size="12">MH-12-RN-8821</text><text x="35" y="140" fill="%2394a3b8" font-family="monospace" font-size="10">Class 12 Heavy Commercial</text></svg>');
  };

  const handleSubmitTagAssignment = (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);
    setSubmitSuccess(null);

    const cleanSerial = tagSerial.trim().toUpperCase();
    const cleanVehicle = vehicleNumber.trim().toUpperCase();

    if (!cleanSerial) {
      setErrorMessage('Tag serial number is required.');
      return;
    }
    if (!cleanVehicle) {
      setErrorMessage('Vehicle registration number is required.');
      return;
    }

    // Execute atomic transaction for single tag assignment
    const res = db.executeTransaction(
      'TAG_ASSIGNED',
      [{
        id: `field-${Date.now()}`,
        subagentId: currentUser.userId,
        subagentName: currentUser.displayName,
        tagClass,
        isRange: false,
        serialNumber: cleanSerial,
        fromSerial: '',
        toSerial: '',
        calculatedQuantity: 1
      }],
      {
        originalFilename: `field_camera_fitment_${cleanSerial}.jpg`,
        rawText: `FIELD TOLL FITMENT SNAP\nSERIAL: ${cleanSerial}\nVEHICLE: ${cleanVehicle}\nCLASS: ${tagClass}\nAGENT: ${currentUser.displayName} (${currentUser.userId})`,
        uploadedBy: `${currentUser.displayName} (Field Agent)`,
        vehicleNumber: cleanVehicle,
        targetExecutiveId: currentUser.userId
      }
    );

    if (res.success) {
      setSubmitSuccess(`Tag ${cleanSerial} successfully installed on ${cleanVehicle} with Proof #${res.proofId}.`);
      onTagCommitted?.(cleanSerial);
    } else {
      setErrorMessage(res.message);
    }
  };

  return (
    <div className="space-y-6 max-w-4xl mx-auto">
      {/* Header */}
      <div>
        <h1 className="text-lg font-bold text-white tracking-tight flex items-center space-x-2">
          <Camera className="w-5 h-5 text-sky-400" />
          <span>FIELD AGENT CAMERA PORTAL</span>
        </h1>
        <p className="text-xs text-slate-400 mt-0.5">
          Scan and affix RFID FASTags to customer vehicles at toll plaza kiosks.
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Left: Camera / Viewfinder */}
        <div className="bg-slate-800/80 border border-slate-700 rounded-2xl p-5 flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between mb-3">
              <span className="text-xs font-bold text-slate-300 uppercase tracking-wider">
                Optical Scanner / Snapshot
              </span>
              <button
                type="button"
                onClick={handleLoadSamplePhoto}
                className="text-[11px] text-amber-400 hover:text-amber-300 font-semibold flex items-center space-x-1"
              >
                <Sparkles className="w-3 h-3" />
                <span>Preset FASTag Snap</span>
              </button>
            </div>

            {isCameraActive ? (
              <div className="relative aspect-4/3 rounded-xl overflow-hidden bg-black flex items-center justify-center">
                <video ref={videoRef} autoPlay playsInline className="w-full h-full object-cover" />
                <div className="absolute inset-x-8 inset-y-12 border-2 border-sky-400/80 rounded-lg pointer-events-none border-dashed" />
              </div>
            ) : photoDataUrl ? (
              <div className="relative aspect-4/3 rounded-xl overflow-hidden bg-slate-950 border border-slate-700 flex items-center justify-center">
                <img src={photoDataUrl} alt="Tag snapshot" className="max-h-full max-w-full object-contain" />
              </div>
            ) : (
              <div className="aspect-4/3 rounded-xl bg-slate-900/60 border border-slate-700/60 flex flex-col items-center justify-center text-center p-6">
                <Camera className="w-12 h-12 text-slate-600 mb-2" />
                <span className="text-xs font-semibold text-slate-300">No active camera stream</span>
                <span className="text-[11px] text-slate-500 mt-1 max-w-xs">
                  Click below to activate device camera or load a preset sample tag image.
                </span>
              </div>
            )}
          </div>

          <div className="pt-4 flex gap-2">
            {isCameraActive ? (
              <>
                <button
                  type="button"
                  onClick={handleCapturePhoto}
                  className="flex-1 py-2 bg-emerald-600 hover:bg-emerald-500 text-white rounded-xl text-xs font-bold shadow-sm"
                >
                  Snap Tag Photo
                </button>
                <button
                  type="button"
                  onClick={() => {
                    if (streamRef.current) streamRef.current.getTracks().forEach(t => t.stop());
                    setIsCameraActive(false);
                  }}
                  className="px-3 py-2 bg-slate-700 text-slate-300 rounded-xl text-xs font-semibold"
                >
                  Cancel
                </button>
              </>
            ) : (
              <button
                type="button"
                onClick={handleStartCamera}
                className="w-full py-2.5 bg-slate-700 hover:bg-slate-600 text-slate-200 rounded-xl text-xs font-semibold flex items-center justify-center space-x-2 transition"
              >
                <Camera className="w-4 h-4 text-sky-400" />
                <span>Start Live Camera Scanner</span>
              </button>
            )}
          </div>
        </div>

        {/* Right: Verification & Fitment Form */}
        <div className="bg-slate-800/80 border border-slate-700 rounded-2xl p-5">
          <h2 className="text-xs font-bold text-slate-300 uppercase tracking-wider mb-4">
            Fitment &amp; Activation Form
          </h2>

          <form onSubmit={handleSubmitTagAssignment} className="space-y-4">
            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1">
                FASTag Serial Number <span className="text-rose-400">*</span>
              </label>
              <input
                type="text"
                value={tagSerial}
                onChange={e => setTagSerial(e.target.value.toUpperCase())}
                placeholder="e.g. 100001"
                className="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-xs font-mono font-bold text-sky-400"
                required
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1">
                Customer Vehicle Plate <span className="text-rose-400">*</span>
              </label>
              <input
                type="text"
                value={vehicleNumber}
                onChange={e => setVehicleNumber(e.target.value.toUpperCase())}
                placeholder="e.g. MH-12-RN-8821"
                className="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-xs font-mono font-bold text-emerald-400"
                required
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-300 mb-1">Vehicle Tag Class</label>
              <select
                value={tagClass}
                onChange={e => setTagClass(e.target.value)}
                className="w-full bg-slate-900 border border-slate-700 rounded-lg px-3 py-2 text-xs text-white"
              >
                <option value="Class 4">Class 4 (Car / Jeep / Van)</option>
                <option value="Class 7">Class 7 (Light Commercial Vehicle)</option>
                <option value="Class 12">Class 12 (Heavy 3-Axle Truck)</option>
                <option value="Class 15">Class 15 (Multi-axle Commercial)</option>
              </select>
            </div>

            <div className="p-3 bg-slate-900/60 rounded-xl border border-slate-700/60 text-xs space-y-1">
              <div className="text-slate-400">Authorized Agent:</div>
              <div className="font-semibold text-white">
                {currentUser.displayName} <span className="text-slate-400 font-mono text-[11px]">({currentUser.userId})</span>
              </div>
              <div className="text-[11px] text-slate-500">{currentUser.region}</div>
            </div>

            {errorMessage && (
              <div className="p-3 rounded-xl bg-rose-950/40 border border-rose-500/50 text-xs text-rose-300 flex items-start space-x-2">
                <AlertCircle className="w-4 h-4 text-rose-400 shrink-0 mt-0.5" />
                <span>{errorMessage}</span>
              </div>
            )}

            {submitSuccess && (
              <div className="p-3 rounded-xl bg-emerald-950/40 border border-emerald-500/50 text-xs text-emerald-300 flex items-start space-x-2">
                <CheckCircle2 className="w-4 h-4 text-emerald-400 shrink-0 mt-0.5" />
                <span>{submitSuccess}</span>
              </div>
            )}

            <button
              type="submit"
              className="w-full py-2.5 bg-emerald-600 hover:bg-emerald-500 text-white rounded-xl text-xs font-bold flex items-center justify-center space-x-2 shadow-sm transition"
            >
              <ShieldCheck className="w-4 h-4" />
              <span>Submit &amp; Install on Vehicle</span>
            </button>
          </form>
        </div>
      </div>
    </div>
  );
};
