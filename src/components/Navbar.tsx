import React from 'react';
import { AppScreen, LoggedInUser } from '../types/inventory';
import {
  LayoutDashboard,
  FileCheck2,
  Search,
  Users,
  ShieldCheck,
  BarChart3,
  Camera,
  PlayCircle,
  LogOut,
  UserCircle2
} from 'lucide-react';

interface NavbarProps {
  currentScreen: AppScreen;
  onNavigate: (screen: AppScreen) => void;
  currentUser: LoggedInUser;
  onSwitchUser: () => void;
  onOpenAcceptanceTests: () => void;
  totalTagsCount: number;
}

export const Navbar: React.FC<NavbarProps> = ({
  currentScreen,
  onNavigate,
  currentUser,
  onSwitchUser,
  onOpenAcceptanceTests,
  totalTagsCount
}) => {
  const isAdmin = currentUser.role === 'ADMIN';

  const navItems: { screen: AppScreen; label: string; icon: React.ReactNode; adminOnly?: boolean }[] = [
    { screen: 'DASHBOARD', label: 'Dashboard', icon: <LayoutDashboard className="w-4 h-4" /> },
    { screen: 'WORKFLOW', label: 'Process Waybill', icon: <FileCheck2 className="w-4 h-4" /> },
    { screen: 'SEARCH', label: 'Tag Search', icon: <Search className="w-4 h-4" /> },
    { screen: 'EXECUTIVES', label: 'Subagents', icon: <Users className="w-4 h-4" />, adminOnly: true },
    { screen: 'PROOFS', label: 'Proof Vault', icon: <ShieldCheck className="w-4 h-4" /> },
    { screen: 'REPORTS', label: 'Audit Reports', icon: <BarChart3 className="w-4 h-4" /> },
    { screen: 'USER_UPLOAD', label: 'Field Camera', icon: <Camera className="w-4 h-4" /> }
  ];

  return (
    <header className="bg-slate-900 border-b border-slate-800 sticky top-0 z-40">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          {/* Brand Logo & Title */}
          <div className="flex items-center space-x-3 cursor-pointer" onClick={() => onNavigate('DASHBOARD')}>
            <div className="w-9 h-9 rounded-xl bg-gradient-to-tr from-sky-600 to-cyan-500 flex items-center justify-center text-white shadow-md shadow-sky-900/30">
              <span className="font-mono font-black text-sm tracking-wider">FT</span>
            </div>
            <div>
              <div className="flex items-center space-x-2">
                <span className="font-bold text-base text-white tracking-tight">FASTag Inventory</span>
                <span className="px-1.5 py-0.2 rounded text-[10px] font-mono bg-sky-500/20 text-sky-400 border border-sky-500/30">
                  v2.0
                </span>
              </div>
              <p className="text-[11px] text-slate-400">Electronic Toll RFID Management</p>
            </div>
          </div>

          {/* Navigation Links (Desktop) */}
          <nav className="hidden md:flex items-center space-x-1">
            {navItems.map(item => {
              if (item.adminOnly && !isAdmin) return null;
              const isActive = currentScreen === item.screen;
              return (
                <button
                  key={item.screen}
                  onClick={() => onNavigate(item.screen)}
                  className={`flex items-center space-x-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold transition ${
                    isActive
                      ? 'bg-sky-500/15 text-sky-300 border border-sky-500/30 shadow-xs'
                      : 'text-slate-400 hover:text-slate-200 hover:bg-slate-800'
                  }`}
                >
                  {item.icon}
                  <span>{item.label}</span>
                </button>
              );
            })}
          </nav>

          {/* Right Controls: Test Suite & User Profile */}
          <div className="flex items-center space-x-2.5">
            {/* Acceptance Test Button */}
            <button
              onClick={onOpenAcceptanceTests}
              className="flex items-center space-x-1.5 px-2.5 py-1.5 rounded-lg bg-emerald-500/15 hover:bg-emerald-500/25 border border-emerald-500/30 text-emerald-300 text-xs font-bold transition shadow-xs"
              title="Open Automated Acceptance Tests A to G"
            >
              <PlayCircle className="w-3.5 h-3.5" />
              <span className="hidden sm:inline">Tests A–G</span>
            </button>

            {/* Total Tags Pill */}
            <div className="hidden lg:flex items-center px-2.5 py-1 rounded-md bg-slate-800 border border-slate-700/60 text-xs text-slate-300">
              <span className="text-slate-400 mr-1.5">Live:</span>
              <span className="font-mono font-bold text-sky-400">{totalTagsCount} tags</span>
            </div>

            {/* User Switcher Pill */}
            <button
              onClick={onSwitchUser}
              className="flex items-center space-x-2 px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-750 border border-slate-700 text-xs transition"
              title="Switch user role"
            >
              <UserCircle2 className="w-4 h-4 text-slate-300" />
              <div className="text-left hidden sm:block">
                <div className="font-semibold text-slate-200 leading-tight text-[11px] truncate max-w-[100px]">
                  {currentUser.displayName}
                </div>
                <div className="text-[10px] text-slate-400 font-mono">
                  {currentUser.role}
                </div>
              </div>
              <LogOut className="w-3 h-3 text-slate-400 ml-1" />
            </button>
          </div>
        </div>
      </div>

      {/* Mobile Screen Selector (Sub-bar) */}
      <div className="md:hidden flex items-center overflow-x-auto px-4 py-2 bg-slate-900/90 border-t border-slate-800 gap-1 scrollbar-none">
        {navItems.map(item => {
          if (item.adminOnly && !isAdmin) return null;
          const isActive = currentScreen === item.screen;
          return (
            <button
              key={item.screen}
              onClick={() => onNavigate(item.screen)}
              className={`flex items-center space-x-1 px-2.5 py-1 rounded-md text-xs whitespace-nowrap font-medium transition ${
                isActive
                  ? 'bg-sky-500/20 text-sky-300 border border-sky-500/40'
                  : 'text-slate-400 hover:text-slate-200'
              }`}
            >
              {item.icon}
              <span>{item.label}</span>
            </button>
          );
        })}
      </div>
    </header>
  );
};
