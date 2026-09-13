import React from 'react';
import { LoggedInUser, UserRole } from '../types/inventory';
import { ShieldCheck, UserCheck, ArrowRight, Layers } from 'lucide-react';

interface LoginScreenProps {
  onSelectUser: (user: LoggedInUser) => void;
  onCancel?: () => void;
}

export const LoginScreen: React.FC<LoginScreenProps> = ({ onSelectUser, onCancel }) => {
  const users: LoggedInUser[] = [
    {
      role: 'ADMIN',
      userId: 'ADMIN-01',
      displayName: 'Central Admin Auditor',
      region: 'National Highway Control Hub'
    },
    {
      role: 'USER',
      userId: 'EXEC-03',
      displayName: 'Suresh Patel',
      region: 'West Tollways Hub'
    },
    {
      role: 'USER',
      userId: 'EXEC-01',
      displayName: 'Kumar S.',
      region: 'North Corridor Hub'
    }
  ];

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-xs p-4">
      <div className="bg-slate-800 border border-slate-700 rounded-2xl max-w-md w-full p-6 shadow-2xl space-y-6">
        <div className="text-center space-y-2">
          <div className="w-12 h-12 rounded-2xl bg-gradient-to-tr from-sky-600 to-cyan-500 flex items-center justify-center text-white mx-auto shadow-lg shadow-sky-600/30">
            <ShieldCheck className="w-6 h-6" />
          </div>
          <h2 className="text-lg font-bold text-white tracking-tight">FASTag Access Control</h2>
          <p className="text-xs text-slate-400">Select an operational profile to interact with the register.</p>
        </div>

        <div className="space-y-3">
          {users.map(u => (
            <button
              key={u.userId}
              onClick={() => onSelectUser(u)}
              className="w-full text-left p-4 rounded-xl bg-slate-900/60 hover:bg-slate-750 border border-slate-700/80 hover:border-sky-500/50 transition flex items-center justify-between group"
            >
              <div className="flex items-center space-x-3">
                <div className={`p-2 rounded-lg ${u.role === 'ADMIN' ? 'bg-sky-500/10 text-sky-400' : 'bg-purple-500/10 text-purple-400'}`}>
                  {u.role === 'ADMIN' ? <Layers className="w-5 h-5" /> : <UserCheck className="w-5 h-5" />}
                </div>
                <div>
                  <div className="flex items-center space-x-2">
                    <span className="font-bold text-sm text-white">{u.displayName}</span>
                    <span className={`text-[10px] font-bold px-1.5 py-0.2 rounded ${
                      u.role === 'ADMIN'
                        ? 'bg-sky-500/20 text-sky-300 border border-sky-500/30'
                        : 'bg-purple-500/20 text-purple-300 border border-purple-500/30'
                    }`}>
                      {u.role}
                    </span>
                  </div>
                  <p className="text-xs text-slate-400 mt-0.5">{u.region} • <span className="font-mono text-[11px]">{u.userId}</span></p>
                </div>
              </div>
              <ArrowRight className="w-4 h-4 text-slate-500 group-hover:text-sky-400 transition" />
            </button>
          ))}
        </div>

        {onCancel && (
          <button
            onClick={onCancel}
            className="w-full py-2 text-xs font-semibold text-slate-400 hover:text-white"
          >
            Cancel
          </button>
        )}
      </div>
    </div>
  );
};
