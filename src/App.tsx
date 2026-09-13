import React, { useState, useEffect } from 'react';
import { db } from './data/db';
import { AppScreen, LoggedInUser, ProofType } from './types/inventory';
import { Navbar } from './components/Navbar';
import { TagDetailModal } from './components/TagDetailModal';
import { ProofDetailModal } from './components/ProofDetailModal';
import { AcceptanceTestDialog } from './components/AcceptanceTestDialog';
import { LoginScreen } from './screens/LoginScreen';
import { DashboardScreen } from './screens/DashboardScreen';
import { WorkflowScreen } from './screens/WorkflowScreen';
import { TagSearchScreen } from './screens/TagSearchScreen';
import { ExecutivesScreen } from './screens/ExecutivesScreen';
import { ProofsScreen } from './screens/ProofsScreen';
import { ReportsScreen } from './screens/ReportsScreen';
import { UserUploadScreen } from './screens/UserUploadScreen';

export default function App() {
  const [currentScreen, setCurrentScreen] = useState<AppScreen>('DASHBOARD');
  const [dbVersion, setDbVersion] = useState(0);

  // User session
  const [currentUser, setCurrentUser] = useState<LoggedInUser>({
    role: 'ADMIN',
    userId: 'ADMIN-01',
    displayName: 'Central Admin Auditor',
    region: 'National Highway Control Hub'
  });

  // Workflow initialization helpers
  const [workflowInitialProofType, setWorkflowInitialProofType] = useState<ProofType>('CENTRAL_RECEIVED');
  const [workflowInitialSampleId, setWorkflowInitialSampleId] = useState<string | undefined>('sample-central-inward');

  // Search initial query
  const [searchInitialQuery, setSearchInitialQuery] = useState('');

  // Modals
  const [inspectedTagSerial, setInspectedTagSerial] = useState<string | null>(null);
  const [inspectedProofId, setInspectedProofId] = useState<number | null>(null);
  const [isAcceptanceTestOpen, setIsAcceptanceTestOpen] = useState(false);
  const [isLoginModalOpen, setIsLoginModalOpen] = useState(false);

  // Subscribe to reactive database changes
  useEffect(() => {
    const unsubscribe = db.subscribe(() => {
      setDbVersion(v => v + 1);
    });
    return unsubscribe;
  }, []);

  const handleStartWorkflow = (proofType: ProofType, sampleId?: string) => {
    setWorkflowInitialProofType(proofType);
    setWorkflowInitialSampleId(sampleId);
    setCurrentScreen('WORKFLOW');
  };

  const handleInspectTag = (serial: string) => {
    setInspectedTagSerial(serial);
  };

  const handleInspectProof = (proofId: number) => {
    setInspectedProofId(proofId);
  };

  const handleInspectExecutiveTags = (executiveId: string) => {
    setSearchInitialQuery(executiveId);
    setCurrentScreen('SEARCH');
  };

  const totalTagsCount = db.getAllTags().length;

  return (
    <div className="min-h-screen bg-slate-900 text-slate-100 flex flex-col antialiased">
      {/* Top Navbar */}
      <Navbar
        currentScreen={currentScreen}
        onNavigate={screen => {
          if (screen === 'SEARCH') setSearchInitialQuery('');
          setCurrentScreen(screen);
        }}
        currentUser={currentUser}
        onSwitchUser={() => setIsLoginModalOpen(true)}
        onOpenAcceptanceTests={() => setIsAcceptanceTestOpen(true)}
        totalTagsCount={totalTagsCount}
      />

      {/* Main Content Area */}
      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-6">
        {currentScreen === 'DASHBOARD' && (
          <DashboardScreen
            onNavigate={setCurrentScreen}
            onStartWorkflow={handleStartWorkflow}
            onInspectTag={handleInspectTag}
            onInspectProof={handleInspectProof}
          />
        )}

        {currentScreen === 'WORKFLOW' && (
          <WorkflowScreen
            initialProofType={workflowInitialProofType}
            initialSampleId={workflowInitialSampleId}
            onViewTagSearch={query => {
              if (query) setSearchInitialQuery(query);
              setCurrentScreen('SEARCH');
            }}
            onViewProofVault={proofId => {
              if (proofId) {
                setInspectedProofId(proofId);
              }
              setCurrentScreen('PROOFS');
            }}
          />
        )}

        {currentScreen === 'SEARCH' && (
          <TagSearchScreen
            initialQuery={searchInitialQuery}
            onSelectTag={handleInspectTag}
            onInspectProof={handleInspectProof}
          />
        )}

        {currentScreen === 'EXECUTIVES' && (
          <ExecutivesScreen
            onInspectExecutiveTags={handleInspectExecutiveTags}
            onOpenAcceptanceTests={() => setIsAcceptanceTestOpen(true)}
          />
        )}

        {currentScreen === 'PROOFS' && (
          <ProofsScreen onInspectProof={handleInspectProof} />
        )}

        {currentScreen === 'REPORTS' && (
          <ReportsScreen />
        )}

        {currentScreen === 'USER_UPLOAD' && (
          <UserUploadScreen
            currentUser={currentUser}
            onTagCommitted={serial => {
              handleInspectTag(serial);
            }}
          />
        )}
      </main>

      {/* Modals & Dialogs */}
      {inspectedTagSerial && (
        <TagDetailModal
          serial={inspectedTagSerial}
          onClose={() => setInspectedTagSerial(null)}
          onInspectProof={proofId => {
            setInspectedTagSerial(null);
            setInspectedProofId(proofId);
          }}
        />
      )}

      {inspectedProofId !== null && (
        <ProofDetailModal
          proofId={inspectedProofId}
          onClose={() => setInspectedProofId(null)}
        />
      )}

      {isAcceptanceTestOpen && (
        <AcceptanceTestDialog
          onClose={() => setIsAcceptanceTestOpen(false)}
          onRefreshView={() => setDbVersion(v => v + 1)}
        />
      )}

      {isLoginModalOpen && (
        <LoginScreen
          onSelectUser={user => {
            setCurrentUser(user);
            setIsLoginModalOpen(false);
          }}
          onCancel={() => setIsLoginModalOpen(false)}
        />
      )}
    </div>
  );
}
