export interface SampleDocument {
  id: string;
  name: string;
  category: string;
  description: string;
  suggestedType: 'EXISTING_STOCK' | 'CENTRAL_RECEIVED' | 'COURIER_TO_EXECUTIVE' | 'TAG_ASSIGNED';
  rawText: string;
  previewSvg: string;
}

export const SAMPLE_DOCUMENTS: SampleDocument[] = [
  {
    id: 'sample-central-inward',
    name: 'Central Inward Manifest (Class 12 & Class 7)',
    category: 'Warehouse Inward',
    description: 'Central manufacturing hub shipment receipt with serial ranges for Class 12 & 7 tags',
    suggestedType: 'CENTRAL_RECEIVED',
    rawText: `=====================================================
NATIONAL ELECTRONIC TOLL COLLECTION (NETC)
CENTRAL INWARD STOCK DELIVERY CHALLAN
Challan Ref: NPCI/DEL/2026/0912
Date: 12-SEP-2026
Issuer Bank: ICICI BANK FASTAG DIVISION
Origin Hub: Central Logistics Facility, Mumbai
Destination: Master Inventory Warehouse, Delhi
Carrier: Blue Dart Aviation Express (AWB #889210482)
Authorized Subagent: SA-1001 (Kumar S. - North Corridor)

ITEMIZED FASTAG INVENTORY DISPATCH:
-----------------------------------------------------
Item 1:
Tag Class: Class 12 (Heavy Commercial Vehicle)
Tag Serial Range: 100001 to 100050
Batch Count: 50 Units
Status: Sealed & Verified

Item 2:
Tag Class: Class 7 (Light Commercial Vehicle)
Tag Serial Range: 100051 to 100075
Batch Count: 25 Units
Status: Sealed & Verified

Total Consignment: 75 Tags
Security Seal: SEC-8829-OK
Inspector Signature: Verified Digital Stamp
=====================================================`,
    previewSvg: `<svg viewBox="0 0 400 240" xmlns="http://www.w3.org/2000/svg" class="w-full h-full">
      <rect width="400" height="240" fill="#0f172a" rx="8"/>
      <rect x="20" y="18" width="360" height="32" rx="4" fill="#1e293b"/>
      <text x="30" y="38" fill="#38bdf8" font-family="monospace" font-size="12" font-weight="bold">NETC CENTRAL DELIVERY CHALLAN</text>
      <text x="310" y="38" fill="#94a3b8" font-family="monospace" font-size="10">ICICI FASTAG</text>
      <line x1="20" y1="60" x2="380" y2="60" stroke="#334155" stroke-dasharray="4"/>
      <text x="30" y="85" fill="#f8fafc" font-size="11" font-family="monospace">FROM: Central Warehouse (Mumbai)</text>
      <text x="30" y="105" fill="#f8fafc" font-size="11" font-family="monospace">TO: Master Hub (Delhi)</text>
      <rect x="30" y="125" width="340" height="40" rx="4" fill="#0369a1" fill-opacity="0.2" stroke="#0284c7"/>
      <text x="40" y="142" fill="#38bdf8" font-family="monospace" font-size="11" font-weight="bold">CLASS 12: 100001 -> 100050 (50 Tags)</text>
      <text x="40" y="157" fill="#94a3b8" font-family="monospace" font-size="10">Class 7:  100051 -> 100075 (25 Tags)</text>
      <text x="30" y="195" fill="#10b981" font-family="monospace" font-size="11">✓ VERIFIED BARCODE: 8904200001</text>
      <rect x="280" y="185" width="90" height="26" rx="4" fill="#10b981" fill-opacity="0.2"/>
      <text x="296" y="202" fill="#34d399" font-family="monospace" font-size="10" font-weight="bold">APPROVED</text>
    </svg>`
  },
  {
    id: 'sample-courier-dispatch',
    name: 'Courier Dispatch Waybill (EXEC-03 Suresh Patel)',
    category: 'Field Courier',
    description: 'Dispatch manifest from Master Inventory to Executive Suresh Patel (EXEC-03)',
    suggestedType: 'COURIER_TO_EXECUTIVE',
    rawText: `=====================================================
FASTAG MASTER LOGISTICS - OUTWARD COURIER WAYBILL
Waybill #: CW-9938210
Date: 12-SEP-2026 14:30
Consignor: Master Central Store Room A
Consignee Subagent: Suresh Patel (EXEC-03)
Region / Route: West Tollways Hub, Plaza 4
Issuer Bank: AXIS BANK FASTAG
Courier Partner: DTDC Secure Courier Service

DISPATCHED ASSETS:
-----------------------------------------------------
Class: Class 12
Serial Number Range: 100001 to 100010
Total Quantity Dispatched: 10 RFID FASTag Units
Subagent ID: EXEC-03
Subagent Name: Suresh Patel

Receiver Acknowledgement: Pending Field Delivery
Tracking Number: DTDC-IN-78392110
=====================================================`,
    previewSvg: `<svg viewBox="0 0 400 240" xmlns="http://www.w3.org/2000/svg" class="w-full h-full">
      <rect width="400" height="240" fill="#0f172a" rx="8"/>
      <rect x="20" y="18" width="360" height="32" rx="4" fill="#1e293b"/>
      <text x="30" y="38" fill="#a855f7" font-family="monospace" font-size="12" font-weight="bold">OUTWARD COURIER WAYBILL</text>
      <text x="290" y="38" fill="#94a3b8" font-family="monospace" font-size="10">EXEC-03 DISPATCH</text>
      <text x="30" y="85" fill="#f8fafc" font-size="11" font-family="monospace">SUBAGENT: EXEC-03 • Suresh Patel</text>
      <text x="30" y="105" fill="#94a3b8" font-size="11" font-family="monospace">DESTINATION: West Tollways Hub</text>
      <rect x="30" y="125" width="340" height="40" rx="4" fill="#7e22ce" fill-opacity="0.2" stroke="#9333ea"/>
      <text x="40" y="145" fill="#c084fc" font-family="monospace" font-size="11" font-weight="bold">100001 to 100010 (10 Tags)</text>
      <text x="40" y="158" fill="#94a3b8" font-family="monospace" font-size="10">Class 12 Heavy Commercial Vehicle</text>
      <text x="30" y="195" fill="#38bdf8" font-family="monospace" font-size="11">AWB: DTDC-IN-78392110</text>
      <rect x="270" y="185" width="100" height="26" rx="4" fill="#0284c7" fill-opacity="0.2"/>
      <text x="282" y="202" fill="#38bdf8" font-family="monospace" font-size="10" font-weight="bold">IN-TRANSIT</text>
    </svg>`
  },
  {
    id: 'sample-tag-assigned',
    name: 'Customer Vehicle Installation Slip (Tags 100001 - 100005)',
    category: 'Toll Installation',
    description: 'Field toll plaza assignment slip installing FASTags onto vehicles',
    suggestedType: 'TAG_ASSIGNED',
    rawText: `=====================================================
TOLL PLAZA FASTAG AFFIXATION & ACTIVATION SLIP
Transaction ID: ACT-2026-7781
Date of Fitment: 12-SEP-2026
Toll Plaza: Western Expressway KM 42
Field Executive: Suresh Patel (EXEC-03)
Issuing Partner: STATE BANK OF INDIA FASTAG

TAG FITMENT DETAILS:
-----------------------------------------------------
Serial Number: 100001 to 100005
Class: Class 12
Quantity: 5 Tags
Target Vehicle: MH-12-RN-8821 (Multi-axle Volvo Truck)
Vehicle Registration: MH-12-RN-8821
Owner / Fleet: Intercity Logistics Corp
Chassis #: MAT628100L9A721

RFID Status: Affixed on Windshield & Activated
Antenna Frequency Check: 865-867 MHz PASSED
=====================================================`,
    previewSvg: `<svg viewBox="0 0 400 240" xmlns="http://www.w3.org/2000/svg" class="w-full h-full">
      <rect width="400" height="240" fill="#0f172a" rx="8"/>
      <rect x="20" y="18" width="360" height="32" rx="4" fill="#1e293b"/>
      <text x="30" y="38" fill="#10b981" font-family="monospace" font-size="12" font-weight="bold">TOLL PLAZA FITMENT SLIP</text>
      <text x="310" y="38" fill="#94a3b8" font-family="monospace" font-size="10">SBI FASTAG</text>
      <text x="30" y="85" fill="#f8fafc" font-size="11" font-family="monospace">VEHICLE: MH-12-RN-8821</text>
      <text x="30" y="105" fill="#94a3b8" font-size="11" font-family="monospace">EXECUTIVE: Suresh Patel (EXEC-03)</text>
      <rect x="30" y="125" width="340" height="40" rx="4" fill="#059669" fill-opacity="0.2" stroke="#10b981"/>
      <text x="40" y="145" fill="#34d399" font-family="monospace" font-size="11" font-weight="bold">100001 to 100005 (5 Tags)</text>
      <text x="40" y="158" fill="#94a3b8" font-family="monospace" font-size="10">Class 12 - Affixed and Activated</text>
      <text x="30" y="195" fill="#10b981" font-family="monospace" font-size="11">✓ RFID EPC CHIP PROGRAMMED</text>
      <rect x="280" y="185" width="90" height="26" rx="4" fill="#10b981" fill-opacity="0.2"/>
      <text x="298" y="202" fill="#34d399" font-family="monospace" font-size="10" font-weight="bold">ASSIGNED</text>
    </svg>`
  },
  {
    id: 'sample-tabular-manifest',
    name: 'Multi-Class Tabular Manifest (Class 4, 7, 12)',
    category: 'Batch Manifest',
    description: 'Tabular shipment breakdown covering passenger car (Class 4) and commercial tags',
    suggestedType: 'EXISTING_STOCK',
    rawText: `=====================================================
NATIONAL HIGHWAYS LOGISTICS RECORD - FASTAG MANIFEST
Batch ID: BATCH-2026-AUG
Origin: Warehouse Stock Baseline
Location: Master Inventory

TABULAR STOCK BREAKDOWN:
TagClass    FromSerial    ToSerial      Quantity   Subagent
Class 4     400001        400030        30         EXEC-01
Class 7     700001        700020        20         EXEC-02
Class 12    900001        900015        15         EXEC-03

Total Serial Units Count: 65 Tags
Audit Signature: Verified Central Ledger
=====================================================`,
    previewSvg: `<svg viewBox="0 0 400 240" xmlns="http://www.w3.org/2000/svg" class="w-full h-full">
      <rect width="400" height="240" fill="#0f172a" rx="8"/>
      <rect x="20" y="18" width="360" height="32" rx="4" fill="#1e293b"/>
      <text x="30" y="38" fill="#f59e0b" font-family="monospace" font-size="12" font-weight="bold">TABULAR MULTI-CLASS MANIFEST</text>
      <text x="300" y="38" fill="#94a3b8" font-family="monospace" font-size="10">65 TAGS</text>
      <text x="30" y="80" fill="#f8fafc" font-size="11" font-family="monospace">Class 4:  400001 -> 400030 (30 Tags)</text>
      <text x="30" y="100" fill="#f8fafc" font-size="11" font-family="monospace">Class 7:  700001 -> 700020 (20 Tags)</text>
      <text x="30" y="120" fill="#f8fafc" font-size="11" font-family="monospace">Class 12: 900001 -> 900015 (15 Tags)</text>
      <rect x="30" y="145" width="340" height="35" rx="4" fill="#d97706" fill-opacity="0.2" stroke="#f59e0b"/>
      <text x="40" y="167" fill="#fbbf24" font-family="monospace" font-size="11" font-weight="bold">All 3 Classes Pre-formatted for Batch Inward</text>
      <text x="30" y="202" fill="#94a3b8" font-family="monospace" font-size="11">TOTAL UNITS: 65 RFID TAGS</text>
    </svg>`
  }
];
