import React, { useState, useMemo } from 'react';
import { db } from '../data/db';
import { InventoryTagEntity, InventoryLocation } from '../types/inventory';
import { LocationBadge } from '../components/LocationBadge';
import { Search, X, Tag, Truck, User, ShieldCheck, Filter } from 'lucide-react';

interface TagSearchScreenProps {
  initialQuery?: string;
  onSelectTag: (serial: string) => void;
  onInspectProof: (proofId: number) => void;
}

export const TagSearchScreen: React.FC<TagSearchScreenProps> = ({
  initialQuery = '',
  onSelectTag,
  onInspectProof
}) => {
  const [searchQuery, setSearchQuery] = useState(initialQuery);
  const [selectedLocation, setSelectedLocation] = useState<string>('');
  const [selectedClass, setSelectedClass] = useState<string>('');

  const allTags = db.getAllTags();

  // Extract unique classes
  const discoveredClasses = useMemo(() => {
    const set = new Set<string>();
    allTags.forEach(t => {
      if (t.tagClass) set.add(t.tagClass);
    });
    return Array.from(set).sort();
  }, [allTags]);

  // Filtered tags
  const searchResults = useMemo(() => {
    return db.searchTags(searchQuery, selectedLocation, selectedClass);
  }, [searchQuery, selectedLocation, selectedClass, allTags]);

  return (
    <div className="space-y-5 max-w-6xl mx-auto">
      {/* Header */}
      <div>
        <h1 className="text-lg font-bold text-white tracking-tight flex items-center space-x-2">
          <Search className="w-5 h-5 text-sky-400" />
          <span>TAG SEARCH &amp; AUDIT REGISTER</span>
        </h1>
        <p className="text-xs text-slate-400 mt-0.5">
          Real-time indexed search across RFID serials, subagent allocations, and vehicle fitments.
        </p>
      </div>

      {/* Search Input Field */}
      <div className="relative">
        <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400">
          <Search className="w-4 h-4" />
        </div>
        <input
          type="text"
          value={searchQuery}
          onChange={e => setSearchQuery(e.target.value)}
          placeholder="Search by Tag Serial (e.g. 100001), Vehicle Number, or Subagent ID..."
          className="w-full bg-slate-800/90 border border-slate-700/80 rounded-xl pl-10 pr-10 py-3 text-xs sm:text-sm text-white placeholder-slate-400 focus:outline-hidden focus:border-sky-500 transition shadow-inner"
        />
        {searchQuery && (
          <button
            onClick={() => setSearchQuery('')}
            className="absolute inset-y-0 right-0 pr-3.5 flex items-center text-slate-400 hover:text-white"
          >
            <X className="w-4 h-4" />
          </button>
        )}
      </div>

      {/* Filter Chips Bar */}
      <div className="space-y-2.5">
        {/* Location Chips */}
        <div className="flex items-center gap-1.5 overflow-x-auto pb-1 scrollbar-none">
          <span className="text-[11px] font-semibold text-slate-400 mr-1 flex items-center">
            <Filter className="w-3 h-3 mr-1" /> Location:
          </span>
          <button
            onClick={() => setSelectedLocation('')}
            className={`px-3 py-1 rounded-full text-xs font-semibold whitespace-nowrap transition ${
              selectedLocation === ''
                ? 'bg-sky-500 text-white shadow-xs'
                : 'bg-slate-800 text-slate-300 hover:bg-slate-750'
            }`}
          >
            All Locations ({allTags.length})
          </button>
          {(['CENTRAL', 'MASTER', 'EXECUTIVE', 'ASSIGNED'] as InventoryLocation[]).map(loc => {
            const count = allTags.filter(t => t.currentLocation === loc).length;
            const isSelected = selectedLocation === loc;
            return (
              <button
                key={loc}
                onClick={() => setSelectedLocation(isSelected ? '' : loc)}
                className={`px-3 py-1 rounded-full text-xs font-semibold whitespace-nowrap transition ${
                  isSelected
                    ? 'bg-sky-500 text-white shadow-xs'
                    : 'bg-slate-800 text-slate-300 hover:bg-slate-750'
                }`}
              >
                {loc} ({count})
              </button>
            );
          })}
        </div>

        {/* Class Chips */}
        {discoveredClasses.length > 0 && (
          <div className="flex items-center gap-1.5 overflow-x-auto pb-1 scrollbar-none">
            <span className="text-[11px] font-semibold text-slate-400 mr-1">Class:</span>
            <button
              onClick={() => setSelectedClass('')}
              className={`px-2.5 py-0.5 rounded-full text-xs font-semibold whitespace-nowrap transition ${
                selectedClass === ''
                  ? 'bg-slate-600 text-white'
                  : 'bg-slate-800/80 text-slate-400 hover:bg-slate-750'
              }`}
            >
              All Classes
            </button>
            {discoveredClasses.map(cls => {
              const count = allTags.filter(t => t.tagClass === cls).length;
              const isSelected = selectedClass === cls;
              return (
                <button
                  key={cls}
                  onClick={() => setSelectedClass(isSelected ? '' : cls)}
                  className={`px-2.5 py-0.5 rounded-full text-xs font-semibold whitespace-nowrap transition ${
                    isSelected
                      ? 'bg-slate-600 text-white'
                      : 'bg-slate-800/80 text-slate-400 hover:bg-slate-750'
                  }`}
                >
                  {cls} ({count})
                </button>
              );
            })}
          </div>
        )}
      </div>

      {/* Results Header Count */}
      <div className="text-xs text-slate-400 flex items-center justify-between">
        <span>Found <strong className="text-white font-mono">{searchResults.length}</strong> matching tags</span>
        {(searchQuery || selectedLocation || selectedClass) && (
          <button
            onClick={() => {
              setSearchQuery('');
              setSelectedLocation('');
              setSelectedClass('');
            }}
            className="text-sky-400 hover:text-sky-300 text-xs font-semibold"
          >
            Clear all filters
          </button>
        )}
      </div>

      {/* Results List */}
      {searchResults.length === 0 ? (
        <div className="bg-slate-800/40 border border-slate-700/60 rounded-2xl p-12 text-center">
          <Tag className="w-10 h-10 text-slate-600 mx-auto mb-2" />
          <h3 className="text-sm font-bold text-slate-300">No matching tags found</h3>
          <p className="text-xs text-slate-500 mt-1 max-w-sm mx-auto">
            Try adjusting your search query, clearing filters, or uploading new stock in the Process Waybill workflow.
          </p>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
          {searchResults.map(tag => (
            <div
              key={tag.tagSerialNumber}
              onClick={() => onSelectTag(tag.tagSerialNumber)}
              className="bg-slate-800/80 hover:bg-slate-750 border border-slate-700/70 hover:border-sky-500/50 rounded-xl p-4 cursor-pointer transition shadow-xs flex flex-col justify-between"
            >
              <div>
                <div className="flex items-center justify-between mb-2">
                  <span className="font-mono font-bold text-sm text-sky-400 tracking-wide">
                    {tag.tagSerialNumber}
                  </span>
                  <LocationBadge location={tag.currentLocation} size="sm" />
                </div>

                <div className="space-y-1.5 text-xs text-slate-300">
                  <div className="flex items-center justify-between">
                    <span className="text-slate-400">Class:</span>
                    <span className="font-semibold">{tag.tagClass}</span>
                  </div>

                  {tag.assignedExecutiveId && (
                    <div className="flex items-center justify-between">
                      <span className="text-slate-400">Subagent:</span>
                      <span className="font-semibold text-purple-300">{tag.assignedExecutiveId}</span>
                    </div>
                  )}

                  {tag.vehicleNumber && (
                    <div className="flex items-center justify-between">
                      <span className="text-slate-400">Vehicle:</span>
                      <span className="font-mono font-bold text-emerald-300">{tag.vehicleNumber}</span>
                    </div>
                  )}
                </div>
              </div>

              <div className="mt-3 pt-2 border-t border-slate-700/60 flex items-center justify-between text-[11px] text-slate-400">
                <span>Proof #{tag.lastProofDocumentId}</span>
                <span className="text-sky-400 hover:text-sky-300 font-semibold">Inspect &rarr;</span>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
