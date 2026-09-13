import React from 'react';
import { InventoryLocation } from '../types/inventory';

interface LocationBadgeProps {
  location?: InventoryLocation | string | null;
  className?: string;
  size?: 'sm' | 'md';
}

export const LocationBadge: React.FC<LocationBadgeProps> = ({ location, className = '', size = 'md' }) => {
  const loc = (location || '').toUpperCase();
  const sizeClasses = size === 'sm' ? 'px-2 py-0.5 text-xs' : 'px-2.5 py-1 text-xs font-semibold';

  switch (loc) {
    case 'CENTRAL':
      return (
        <span className={`inline-flex items-center rounded-full bg-amber-500/15 text-amber-300 border border-amber-500/30 ${sizeClasses} ${className}`}>
          <span className="w-1.5 h-1.5 rounded-full bg-amber-400 mr-1.5"></span>
          CENTRAL
        </span>
      );
    case 'MASTER':
      return (
        <span className={`inline-flex items-center rounded-full bg-sky-500/15 text-sky-300 border border-sky-500/30 ${sizeClasses} ${className}`}>
          <span className="w-1.5 h-1.5 rounded-full bg-sky-400 mr-1.5"></span>
          MASTER
        </span>
      );
    case 'EXECUTIVE':
      return (
        <span className={`inline-flex items-center rounded-full bg-purple-500/15 text-purple-300 border border-purple-500/30 ${sizeClasses} ${className}`}>
          <span className="w-1.5 h-1.5 rounded-full bg-purple-400 mr-1.5"></span>
          EXECUTIVE
        </span>
      );
    case 'ASSIGNED':
      return (
        <span className={`inline-flex items-center rounded-full bg-emerald-500/15 text-emerald-300 border border-emerald-500/30 ${sizeClasses} ${className}`}>
          <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 mr-1.5"></span>
          ASSIGNED
        </span>
      );
    default:
      return (
        <span className={`inline-flex items-center rounded-full bg-slate-700/60 text-slate-300 border border-slate-600 ${sizeClasses} ${className}`}>
          {loc || 'UNKNOWN'}
        </span>
      );
  }
};
