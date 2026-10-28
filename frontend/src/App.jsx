import React, { useState, useEffect, useMemo, useRef } from 'react';

const API_BASE = ''; // Uses Vite proxy config to forward to http://localhost:8080

const getAuthHeaders = (auth) => {
  if (!auth || !auth.username || !auth.password) return {};
  const token = btoa(`${auth.username}:${auth.password}`);
  return {
    'Authorization': `Basic ${token}`,
  };
};

const getSavedAuth = () => {
  try {
    const saved = localStorage.getItem('sanvii_hr_auth');
    if (saved) {
      const parsed = JSON.parse(saved);
      if (parsed && parsed.username && parsed.password) {
        return parsed;
      }
    }
  } catch (e) {}
  return null;
};

// ==========================================
// MULTI-ROUND INTERVIEW PIPELINE HELPERS
// ==========================================
export function getNextPendingRound(cand) {
  if (!cand) return 'L1_TECHNICAL';
  const status = cand.status;
  if (status === 'L1_PASSED') return 'L2_TECHNICAL';
  if (status === 'L2_PASSED') return 'MANAGERIAL';
  if (status === 'MANAGERIAL_PASSED') return 'HR_ROUND';
  return 'L1_TECHNICAL';
}

export function formatRoundName(round) {
  const map = {
    L1_TECHNICAL: 'L1: Technical Screening',
    L2_TECHNICAL: 'L2: Advanced Technical & Coding',
    MANAGERIAL: 'Managerial Round',
    HR_ROUND: 'HR Round'
  };
  return map[round] || round || 'Technical Interview';
}

export function getActiveScheduledInterview(cand) {
  if (!cand) return null;
  const list = cand.interviews || [];
  return list.slice().reverse().find(i => i.result === 'PENDING') || null;
}

export function canScheduleInterview(cand) {
  if (!cand) return false;
  const s = cand.status;
  const isEligible = ['SHORTLISTED', 'REVIEW_REQUIRED', 'L1_PASSED', 'L2_PASSED', 'MANAGERIAL_PASSED'].includes(s);
  const hasPending = (cand.interviews || []).some(i => i.result === 'PENDING');
  return isEligible && !hasPending;
}

export function canRecordDecision(cand) {
  if (!cand) return false;
  const s = cand.status;
  return ['L1_SCHEDULED', 'L2_SCHEDULED', 'MANAGERIAL_SCHEDULED', 'HR_SCHEDULED', 'INTERVIEW_SCHEDULED'].includes(s) ||
         (cand.interviews || []).some(i => i.result === 'PENDING');
}

export default function App() {
  const [auth, setAuth] = useState(getSavedAuth);
  const [activeTab, setActiveTab] = useState('dashboard');
  
  // Portal URL detection: Redirect to dedicated Interviewer Portal if URL parameters indicate an interviewer
  useEffect(() => {
    try {
      const params = new URLSearchParams(window.location.search);
      if (params.get('portal') === 'interviewer' || params.get('token') || params.get('interviewId')) {
        window.location.replace('/interviewer.html' + window.location.search);
      }
    } catch (e) {}
  }, []);

  const [registeredInterviewers, setRegisteredInterviewers] = useState([]);

  const [jobs, setJobs] = useState([]);
  const [candidates, setCandidates] = useState([]);
  const [metrics, setMetrics] = useState(null);
  const [gmailStatus, setGmailStatus] = useState(null);
  
  const [toast, setToast] = useState(null);
  const [selectedCandidate, setSelectedCandidate] = useState(null);
  const [candidateDetail, setCandidateDetail] = useState(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [previewEmail, setPreviewEmail] = useState(null);

  // Multi-Round Interview Pipeline Modals
  const [scheduleCandidate, setScheduleCandidate] = useState(null);
  const [scheduleForm, setScheduleForm] = useState({
    round: 'L1_TECHNICAL',
    interviewerName: 'Lead Technical Architect',
    interviewerEmail: 'lead.tech@sanvii.com',
    interviewDate: new Date(Date.now() + 86400000).toISOString().slice(0, 16),
    durationMinutes: 45,
    meetingLink: '',
    notes: 'Core technical concepts, architecture, and problem solving'
  });
  const [submittingSchedule, setSubmittingSchedule] = useState(false);

  const [decisionCandidate, setDecisionCandidate] = useState(null);
  const [decisionInterview, setDecisionInterview] = useState(null);
  const [decisionForm, setDecisionForm] = useState({
    round: 'L1_TECHNICAL',
    result: 'SELECTED',
    interviewerName: 'Lead Technical Architect',
    feedback: 'Candidate demonstrated deep technical mastery and clear communication.',
    technicalRating: 5,
    communicationRating: 4,
    confidenceRating: 4,
    bodyLanguageRating: 5,
    comments: '',
    recommendation: 'RECOMMENDED'
  });
  const [submittingDecision, setSubmittingDecision] = useState(false);

  // Legacy compatibility
  const [interviewCandidate, setInterviewCandidate] = useState(null);
  const [interviewForm, setInterviewForm] = useState({
    result: 'SELECTED',
    interviewerName: 'Lead Architect',
    feedback: 'Candidate demonstrated deep mastery in microservices, Java 21, and database indexing.',
    interviewDate: new Date().toISOString().slice(0, 16)
  });
  const [submittingInterview, setSubmittingInterview] = useState(false);

  const [showCreateJobModal, setShowCreateJobModal] = useState(false);
  const [jobForm, setJobForm] = useState({
    jobTitle: 'Senior Java Spring Boot Developer',
    department: 'Engineering',
    description: 'Building high-scale microservices, distributed workflows, and PostgreSQL data pipelines.',
    requiredSkills: 'Java, Spring Boot, PostgreSQL, REST API, Docker',
    preferredSkills: 'Kubernetes, AWS, Kafka',
    minimumExperience: 4.0,
    educationRequirement: 'Bachelor in Computer Science or related engineering',
    location: 'Remote',
    employmentType: 'FULL_TIME'
  });

  const showToast = (message, type = 'info') => {
    setToast({ message, type });
    setTimeout(() => setToast(null), 4500);
  };

  const fetchData = async () => {
    if (!auth || !auth.username) return;
    try {
      const headers = getAuthHeaders(auth);
      
      fetch(`${API_BASE}/api/v1/dashboard/metrics`, { headers })
        .then(res => {
          if (res.status === 401) {
            try { localStorage.removeItem('sanvii_hr_auth'); } catch(e) {}
            setAuth(null);
            showToast('Session expired. Please log in again.', 'error');
            return null;
          }
          return res.ok ? res.json() : null;
        })
        .then(data => data?.data && setMetrics(data.data))
        .catch(() => {});

      fetch(`${API_BASE}/api/v1/jobs`, { headers })
        .then(res => res.ok ? res.json() : null)
        .then(data => data?.data && setJobs(data.data))
        .catch(() => {});

      fetch(`${API_BASE}/api/v1/candidates?size=50`, { headers })
        .then(res => res.ok ? res.json() : null)
        .then(data => {
          if (data?.data?.content) setCandidates(data.data.content);
          else if (Array.isArray(data?.data)) setCandidates(data.data);
        })
        .catch(() => {});

      fetch(`${API_BASE}/api/v1/gmail/status`, { headers })
        .then(res => res.ok ? res.json() : null)
        .then(data => data?.data && setGmailStatus(data.data))
        .catch(() => {});

      fetch(`${API_BASE}/api/v1/hr/interviewers`, { headers })
        .then(res => res.ok ? res.json() : null)
        .then(data => data?.data && setRegisteredInterviewers(data.data))
        .catch(() => {});
    } catch (err) {
      console.error('Failed fetching data', err);
    }
  };

  useEffect(() => {
    fetchData();
    const interval = setInterval(fetchData, 20000);
    return () => clearInterval(interval);
  }, [auth]);

  const openCandidateDrawer = async (candidate) => {
    setSelectedCandidate(candidate);
    setDetailLoading(true);
    try {
      const res = await fetch(`${API_BASE}/api/v1/candidates/${candidate.id}`, {
        headers: getAuthHeaders(auth)
      });
      if (res.ok) {
        const data = await res.json();
        setCandidateDetail(data.data);
      }
    } catch (e) {
      showToast('Failed to load candidate details', 'error');
    } finally {
      setDetailLoading(false);
    }
  };

  // Open Schedule Interview Modal with auto-detected round
  const openScheduleModal = (cand) => {
    const nextRound = getNextPendingRound(cand);
    const tomorrow = new Date(Date.now() + 24 * 60 * 60 * 1000);
    tomorrow.setHours(11, 0, 0, 0);
    const isoDate = new Date(tomorrow.getTime() - tomorrow.getTimezoneOffset() * 60000).toISOString().slice(0, 16);
    const candCleanName = (cand.fullName || 'Candidate').replace(/[^a-zA-Z0-9]/g, '') || 'Candidate';
    const randomSuffix = Math.random().toString(36).substring(2, 8);
    const liveMeetingRoom = `https://meet.jit.si/Sanvii-Interview-${candCleanName}-${nextRound}-${randomSuffix}`;

    const defaultInterviewers = {
      L1_TECHNICAL: { name: 'Lead Technical Architect', email: 'lead.tech@sanvii.com', notes: 'Core computer science fundamentals, data structures, and problem solving.' },
      L2_TECHNICAL: { name: 'Principal Engineer', email: 'principal.eng@sanvii.com', notes: 'System architecture, API design, live coding, and database tuning.' },
      MANAGERIAL: { name: 'Engineering Director', email: 'director.eng@sanvii.com', notes: 'Project ownership, cross-functional teamwork, and leadership alignment.' },
      HR_ROUND: { name: 'Sanvii HR Admin', email: 'hr@sanvii.com', notes: 'Cultural values, compensation, notice period, and onboarding formalities.' }
    };
    const conf = defaultInterviewers[nextRound] || defaultInterviewers.L1_TECHNICAL;

    setScheduleCandidate(cand);
    setScheduleForm({
      round: nextRound,
      interviewerName: conf.name,
      interviewerEmail: conf.email,
      interviewDate: isoDate,
      durationMinutes: 45,
      meetingLink: liveMeetingRoom,
      notes: conf.notes
    });
  };

  // Open Record Decision Modal
  const openDecisionModal = (cand, interview) => {
    const targetInterview = interview || getActiveScheduledInterview(cand);
    const round = targetInterview?.round || getNextPendingRound(cand);
    setDecisionCandidate(cand);
    setDecisionInterview(targetInterview);
    setDecisionForm({
      round: round,
      result: 'SELECTED',
      interviewerName: targetInterview?.interviewerName || 'Lead Evaluator',
      feedback: `Candidate demonstrated strong competence and exceeded benchmarks for ${formatRoundName(round)}.`
    });
  };

  // Submit Interview Schedule
  const handleScheduleSubmit = async () => {
    if (!scheduleCandidate) return;
    setSubmittingSchedule(true);
    try {
      const payload = {
        round: scheduleForm.round,
        interviewerName: scheduleForm.interviewerName,
        interviewerEmail: scheduleForm.interviewerEmail,
        interviewDate: scheduleForm.interviewDate,
        durationMinutes: parseInt(scheduleForm.durationMinutes) || 45,
        meetingLink: scheduleForm.meetingLink?.trim() || null,
        notes: scheduleForm.notes
      };
      const res = await fetch(`${API_BASE}/api/v1/candidates/${scheduleCandidate.id}/interviews/schedule`, {
        method: 'POST',
        headers: {
          ...getAuthHeaders(auth),
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(payload)
      });
      const json = await res.json();
      if (res.ok && json.success) {
        showToast(json.message || `${formatRoundName(scheduleForm.round)} scheduled! Google Meet invitations dispatched.`, 'success');
        const updated = json.data;
        setScheduleCandidate(null);
        fetchData();
        if (selectedCandidate && selectedCandidate.id === scheduleCandidate.id) {
          openCandidateDrawer(updated || scheduleCandidate);
        }
      } else {
        showToast(json.message || 'Failed to schedule interview round', 'error');
      }
    } catch (e) {
      showToast('Error scheduling interview: ' + e.message, 'error');
    } finally {
      setSubmittingSchedule(false);
    }
  };

  // Submit Round Decision
  const handleDecisionSubmit = async () => {
    if (!decisionCandidate) return;
    setSubmittingDecision(true);
    try {
      const interviewId = decisionInterview?.id;
      const url = interviewId
        ? `${API_BASE}/api/v1/candidates/${decisionCandidate.id}/interviews/${interviewId}/result`
        : `${API_BASE}/api/v1/candidates/${decisionCandidate.id}/interview-result`;

      const payload = {
        result: decisionForm.result,
        interviewerName: decisionForm.interviewerName,
        feedback: decisionForm.feedback || decisionForm.comments,
        comments: decisionForm.comments || decisionForm.feedback,
        technicalRating: decisionForm.technicalRating || 5,
        communicationRating: decisionForm.communicationRating || 4,
        confidenceRating: decisionForm.confidenceRating || 4,
        bodyLanguageRating: decisionForm.bodyLanguageRating || 5,
        recommendation: decisionForm.result === 'SELECTED' ? 'RECOMMENDED' : 'NOT_RECOMMENDED',
        interviewDate: new Date().toISOString()
      };

      const res = await fetch(url, {
        method: 'POST',
        headers: {
          ...getAuthHeaders(auth),
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(payload)
      });
      const json = await res.json();
      if (res.ok && json.success) {
        const isPassed = decisionForm.result === 'SELECTED';
        showToast(
          isPassed 
            ? 'Candidate passed round! Advanced to next stage & email dispatched.' 
            : 'Candidate marked Not Selected. Regret email dispatched.',
          'success'
        );
        const updated = json.data;
        setDecisionCandidate(null);
        setDecisionInterview(null);
        fetchData();
        if (selectedCandidate && selectedCandidate.id === decisionCandidate.id) {
          openCandidateDrawer(updated || decisionCandidate);
        }
      } else {
        showToast(json.message || 'Failed recording interview decision', 'error');
      }
    } catch (e) {
      showToast('Error recording decision: ' + e.message, 'error');
    } finally {
      setSubmittingDecision(false);
    }
  };

  if (!auth) {
    return (
      <LoginPage
        onLogin={(newAuth) => {
          setAuth(newAuth);
          try {
            localStorage.setItem('sanvii_hr_auth', JSON.stringify(newAuth));
          } catch (e) {}
          showToast(`Welcome back, HR Administrator ${newAuth.username}!`, 'success');
        }}
        toast={toast}
        setToast={setToast}
      />
    );
  }

  return (
    <div className="flex flex-col min-h-screen bg-slate-50 text-slate-800">
      {/* Top Navigation Bar with Perfect Alignment & Breathing Room */}
      <header className="bg-slate-900 border-b border-slate-800 text-white sticky top-0 z-30 shadow-lg">
        <div className="max-w-[1650px] w-full mx-auto px-4 sm:px-6 lg:px-8 min-h-16 py-2.5 flex items-center justify-between gap-4">
          {/* Brand Logo & Title: perfectly vertically aligned with shrink-0 and clean whitespace handling */}
          <div className="flex items-center gap-3 shrink-0 py-0.5 min-w-[280px]">
            <div className="w-11 h-11 rounded-xl bg-white flex items-center justify-center shadow-md shadow-blue-500/20 border border-slate-700/50 p-1.5 shrink-0 overflow-hidden">
              <img
                src="/sanvii-icon.png"
                alt="Sanvii Techmet"
                className="w-full h-full object-contain"
                onError={(e) => { if (window.SANVII_ICON_DATA) e.target.src = window.SANVII_ICON_DATA; }}
              />
            </div>
            <div className="flex flex-col justify-center">
              <div className="flex items-center gap-2">
                <span className="font-bold text-base sm:text-lg text-white whitespace-nowrap leading-none tracking-tight">Sanvii ATS Platform</span>
                <span className="text-[10px] sm:text-xs bg-emerald-500/20 text-emerald-300 font-semibold px-2 py-0.5 rounded-full border border-emerald-500/30 whitespace-nowrap leading-tight">
                  React 18 Active
                </span>
              </div>
              <p className="text-[11px] text-slate-400 font-medium whitespace-nowrap mt-1 leading-none tracking-tight">
                SANVII TECHMET &bull; Deploying Excellence, Delivering Success
              </p>
            </div>
          </div>

          <nav className="flex items-center space-x-1 sm:space-x-1.5 overflow-x-auto custom-scrollbar py-1">
            {[
              { id: 'dashboard', label: 'Dashboard', icon: '📊' },
              { id: 'jobs', label: 'Job Openings', icon: '💼' },
              { id: 'candidates', label: 'Candidates', icon: '👥' },
              { id: 'interviewers', label: 'Interviewers', icon: '👨‍💼' },
              { id: 'simulator', label: 'Email Simulator', icon: '✉️' },
              { id: 'gmail', label: 'Gmail Polling', icon: '📬' },
            ].map(tab => (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                className={`px-3 py-2 rounded-xl text-xs sm:text-sm font-semibold transition-all flex items-center gap-1.5 whitespace-nowrap ${
                  activeTab === tab.id
                    ? 'bg-blue-600 text-white shadow-md shadow-blue-600/30 ring-1 ring-blue-400/40'
                    : 'text-slate-300 hover:text-white hover:bg-slate-800/80'
                }`}
              >
                <span>{tab.icon}</span>
                <span>{tab.label}</span>
              </button>
            ))}
          </nav>

          {/* HR Administrator Badge & Controls */}
          <div className="flex items-center gap-2 sm:gap-3 pl-3 sm:pl-4 border-l border-slate-800 shrink-0">
            <div className="flex items-center gap-2">
              <div className="w-8 h-8 rounded-full bg-white flex items-center justify-center shadow-md ring-2 ring-blue-500/30 p-1 overflow-hidden">
                <img
                  src="/sanvii-icon.png"
                  alt="Sanvii"
                  className="w-full h-full object-contain"
                  onError={(e) => { if (window.SANVII_ICON_DATA) e.target.src = window.SANVII_ICON_DATA; }}
                />
              </div>
              <div className="text-right hidden sm:block">
                <div className="text-xs font-bold text-slate-100 flex items-center gap-1.5 justify-end">
                  <span>{auth.username}</span>
                  <span className="text-[10px] font-semibold bg-blue-500/20 text-blue-300 border border-blue-500/30 px-1.5 py-0.2 rounded">
                    HR Admin
                  </span>
                </div>
                <div className="text-[10px] text-emerald-400 flex items-center gap-1 justify-end font-medium">
                  <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse"></span>
                  <span>Active Session</span>
                </div>
              </div>
            </div>
            <button
              onClick={fetchData}
              title="Refresh Data"
              className="p-2 rounded-lg bg-slate-800 text-slate-300 hover:text-white hover:bg-slate-700 transition"
            >
              🔄
            </button>
            <button
              onClick={() => {
                try { localStorage.removeItem('sanvii_hr_auth'); } catch(e) {}
                setAuth(null);
                showToast('HR Administrator logged out successfully.', 'info');
              }}
              title="Sign Out"
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-red-500/10 text-red-400 hover:bg-red-500/20 hover:text-red-300 border border-red-500/20 text-xs font-semibold transition"
            >
              <span>Sign Out</span>
            </button>
          </div>
        </div>
      </header>

      {/* Toast */}
      {toast && (
        <div className={`fixed bottom-6 right-6 z-50 px-5 py-3.5 rounded-xl shadow-2xl border text-sm font-medium flex items-center gap-3 ${
          toast.type === 'error' ? 'bg-red-50 text-red-900 border-red-300' :
          toast.type === 'success' ? 'bg-emerald-50 text-emerald-900 border-emerald-300' :
          'bg-blue-50 text-blue-900 border-blue-300'
        }`}>
          <span>{toast.type === 'error' ? '⚠️' : toast.type === 'success' ? '✅' : 'ℹ️'}</span>
          <span>{toast.message}</span>
          <button onClick={() => setToast(null)} className="ml-2 text-slate-400 hover:text-slate-600 font-bold">&times;</button>
        </div>
      )}

      {/* Main View */}
      <main className="max-w-[1650px] w-full mx-auto px-4 sm:px-6 lg:px-8 py-8 flex-1">
        {activeTab === 'dashboard' && (
          <DashboardTab
            metrics={metrics}
            jobs={jobs}
            candidates={candidates}
            onSelectCandidate={openCandidateDrawer}
            onOpenSchedule={openScheduleModal}
            onOpenDecision={openDecisionModal}
            onGoToSimulator={() => setActiveTab('simulator')}
            onGoToJobs={() => setActiveTab('jobs')}
          />
        )}

        {activeTab === 'jobs' && (
          <JobsTab
            jobs={jobs}
            onOpenCreate={() => setShowCreateJobModal(true)}
            onRefresh={fetchData}
          />
        )}

        {activeTab === 'candidates' && (
          <CandidatesTab
            candidates={candidates}
            jobs={jobs}
            onSelectCandidate={openCandidateDrawer}
            onOpenSchedule={openScheduleModal}
            onOpenDecision={openDecisionModal}
            onInterviewCandidate={(cand) => {
              if (canRecordDecision(cand)) {
                openDecisionModal(cand);
              } else {
                openScheduleModal(cand);
              }
            }}
            onRefresh={fetchData}
          />
        )}

        {activeTab === 'interviewers' && (
          <InterviewersManagementView
            auth={auth}
            interviewers={registeredInterviewers}
            onRefreshInterviewers={fetchData}
            showToast={showToast}
          />
        )}

        {activeTab === 'simulator' && (
          <SimulatorTab
            jobs={jobs}
            auth={auth}
            onSuccess={(newCandidate) => {
              fetchData();
              openCandidateDrawer(newCandidate);
              showToast('Application simulated & email shot successfully!', 'success');
            }}
          />
        )}

        {activeTab === 'gmail' && (
          <GmailTab
            gmailStatus={gmailStatus}
            auth={auth}
            onRefresh={fetchData}
            showToast={showToast}
          />
        )}
      </main>

      {/* Candidate Profile Drawer */}
      {selectedCandidate && (
        <CandidateDrawer
          candidateSummary={selectedCandidate}
          candidateDetail={candidateDetail}
          loading={detailLoading}
          onClose={() => {
            setSelectedCandidate(null);
            setCandidateDetail(null);
          }}
          onPreviewEmail={(email) => setPreviewEmail(email)}
          onOpenSchedule={openScheduleModal}
          onOpenDecision={openDecisionModal}
          onOpenInterview={() => {
            const c = candidateDetail || selectedCandidate;
            if (canRecordDecision(c)) {
              openDecisionModal(c);
            } else {
              openScheduleModal(c);
            }
          }}
          showToast={showToast}
        />
      )}

      {/* Email Preview Modal */}
      {previewEmail && (
        <EmailModal email={previewEmail} onClose={() => setPreviewEmail(null)} />
      )}

      {/* Multi-Round Schedule Interview Modal */}
      {scheduleCandidate && (
        <ScheduleInterviewModal
          candidate={scheduleCandidate}
          form={scheduleForm}
          setForm={setScheduleForm}
          registeredInterviewers={registeredInterviewers}
          submitting={submittingSchedule}
          onClose={() => setScheduleCandidate(null)}
          onSubmit={handleScheduleSubmit}
        />
      )}

      {/* Record Round Decision Modal */}
      {decisionCandidate && (
        <RecordDecisionModal
          candidate={decisionCandidate}
          interview={decisionInterview}
          form={decisionForm}
          setForm={setDecisionForm}
          submitting={submittingDecision}
          onClose={() => {
            setDecisionCandidate(null);
            setDecisionInterview(null);
          }}
          onSubmit={handleDecisionSubmit}
        />
      )}

      {/* Legacy Interview Decision Modal */}
      {interviewCandidate && (
        <InterviewDecisionModal
          candidate={interviewCandidate}
          form={interviewForm}
          setForm={setInterviewForm}
          submitting={submittingInterview}
          onClose={() => setInterviewCandidate(null)}
          onSubmit={async () => {
            setSubmittingInterview(true);
            try {
              const res = await fetch(`${API_BASE}/api/v1/candidates/${interviewCandidate.id}/interview-result`, {
                method: 'POST',
                headers: {
                  ...getAuthHeaders(auth),
                  'Content-Type': 'application/json'
                },
                body: JSON.stringify(interviewForm)
              });
              const json = await res.json();
              if (res.ok && json.success) {
                showToast(`Interview decision recorded! Status updated to ${interviewForm.result} and email shot!`, 'success');
                setInterviewCandidate(null);
                fetchData();
                if (selectedCandidate?.id === interviewCandidate.id) {
                  openCandidateDrawer(interviewCandidate);
                }
              } else {
                showToast(json.message || 'Failed recording interview result', 'error');
              }
            } catch (e) {
              showToast('Error recording interview result: ' + e.message, 'error');
            } finally {
              setSubmittingInterview(false);
            }
          }}
        />
      )}

      {/* Create Job Modal */}
      {showCreateJobModal && (
        <CreateJobDialog
          form={jobForm}
          setForm={setJobForm}
          onClose={() => setShowCreateJobModal(false)}
          onSubmit={async () => {
            try {
              const payload = {
                ...jobForm,
                requiredSkills: jobForm.requiredSkills.split(',').map(s => s.trim()).filter(Boolean),
                preferredSkills: jobForm.preferredSkills.split(',').map(s => s.trim()).filter(Boolean),
                minimumExperience: parseFloat(jobForm.minimumExperience) || 0
              };
              const res = await fetch(`${API_BASE}/api/v1/jobs`, {
                method: 'POST',
                headers: {
                  ...getAuthHeaders(auth),
                  'Content-Type': 'application/json'
                },
                body: JSON.stringify(payload)
              });
              const json = await res.json();
              if (res.ok && json.success) {
                showToast('Job opening created successfully!', 'success');
                setShowCreateJobModal(false);
                fetchData();
              } else {
                showToast(json.message || 'Failed creating job', 'error');
              }
            } catch (e) {
              showToast(e.message, 'error');
            }
          }}
        />
      )}

      <footer className="bg-white border-t border-slate-200 py-6 text-center text-xs text-slate-500">
        <p>Sanvii ATS Platform &bull; React 18 Frontend &bull; Spring Boot 3.3 Backend &bull; Gmail API</p>
      </footer>
    </div>
  );
}

// ----------------- Subcomponents -----------------
function DashboardTab({ metrics, jobs, candidates, onSelectCandidate, onOpenSchedule, onOpenDecision, onGoToSimulator, onGoToJobs }) {
  const stats = [
    { label: 'Total Applications', value: metrics?.totalApplications || metrics?.totalCandidates || candidates.length, color: 'from-blue-600 to-indigo-600', icon: '📑' },
    { label: 'Shortlisted (Stage 1)', value: metrics?.shortlisted || metrics?.shortlistedCount || candidates.filter(c => c.status === 'SHORTLISTED').length, color: 'from-emerald-500 to-teal-600', icon: '⭐' },
    { label: 'Interviews Scheduled', value: metrics?.interviewScheduled || candidates.filter(c => ['L1_SCHEDULED', 'L1_PASSED', 'L2_SCHEDULED', 'L2_PASSED', 'MANAGERIAL_SCHEDULED', 'MANAGERIAL_PASSED', 'HR_SCHEDULED', 'INTERVIEW_SCHEDULED'].includes(c.status)).length, color: 'from-blue-600 to-cyan-600', icon: '🎥' },
    { label: 'Selected / Hired', value: metrics?.selected || metrics?.selectedCount || candidates.filter(c => c.status === 'SELECTED').length, color: 'from-purple-600 to-indigo-700', icon: '🎉' },
    { label: 'Not Selected / Regret', value: (metrics?.notSelected || 0) + (metrics?.rejected || metrics?.rejectedCount || 0) || candidates.filter(c => c.status === 'REJECTED' || c.status === 'NOT_SELECTED').length, color: 'from-rose-500 to-red-600', icon: '🚫' },
  ];

  return (
    <div className="space-y-8">
      <div className="bg-gradient-to-r from-slate-900 via-indigo-950 to-slate-900 rounded-3xl p-8 text-white shadow-xl relative overflow-hidden">
        <div className="relative z-10 max-w-2xl">
          <span className="text-xs uppercase tracking-widest font-bold text-blue-400 bg-blue-900/50 px-3 py-1 rounded-full border border-blue-500/30 inline-block mb-3">
            Full-Stack Automated Talent Pipeline
          </span>
          <h1 className="text-3xl font-extrabold tracking-tight mb-3">
            AI-Driven Resume Screening & Automated Email Dispatch
          </h1>
          <p className="text-slate-300 text-sm leading-relaxed mb-6">
            Parse candidate resumes in PDF/DOCX format, calculate instant ATS match scores, validate inbound email structure (subject, cover note, attachment), and automatically shoot dynamic notification emails via Gmail API.
          </p>
          <div className="flex flex-wrap gap-3">
            <button
              onClick={onGoToSimulator}
              className="bg-blue-600 hover:bg-blue-500 text-white font-semibold px-5 py-2.5 rounded-xl shadow-lg shadow-blue-600/30 transition flex items-center gap-2 text-sm"
            >
              <span>✉️</span>
              <span>Open Email Simulator</span>
            </button>
            <button
              onClick={onGoToJobs}
              className="bg-slate-800 hover:bg-slate-700 text-slate-200 font-semibold px-5 py-2.5 rounded-xl border border-slate-700 transition flex items-center gap-2 text-sm"
            >
              <span>💼</span>
              <span>View {jobs.length} Active Jobs</span>
            </button>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-2 md:grid-cols-5 gap-4">
        {stats.map((s, idx) => (
          <div key={idx} className="bg-white rounded-2xl p-5 border border-slate-200/80 shadow-sm hover:shadow-md transition">
            <div className="flex items-center justify-between mb-2">
              <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">{s.label}</span>
              <span className="text-xl">{s.icon}</span>
            </div>
            <div className="text-3xl font-black text-slate-900">{s.value}</div>
          </div>
        ))}
      </div>

      {/* HR Diversity & Gender Demographics Card */}
      {(() => {
        const male = metrics?.maleCount ?? candidates.filter(c => c.gender === 'MALE').length;
        const female = metrics?.femaleCount ?? candidates.filter(c => c.gender === 'FEMALE').length;
        const other = metrics?.otherCount ?? candidates.filter(c => c.gender === 'OTHER').length;
        const unspecified = metrics?.unspecifiedCount ?? candidates.filter(c => !c.gender || c.gender === 'UNSPECIFIED').length;
        const total = male + female + other + unspecified || (candidates.length || 1);
        
        const malePct = Math.round((male / total) * 100);
        const femalePct = Math.round((female / total) * 100);
        const otherPct = Math.round((other / total) * 100);
        const unspecPct = Math.max(0, 100 - malePct - femalePct - otherPct);

        return (
          <div className="bg-white rounded-3xl p-6 border border-slate-200/90 shadow-sm">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 mb-5">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-2xl bg-gradient-to-tr from-pink-500 to-indigo-600 flex items-center justify-center text-white text-lg shadow-md shadow-indigo-500/20">
                  👥
                </div>
                <div>
                  <h3 className="font-extrabold text-base text-slate-900 tracking-tight">Applicant Diversity & Gender Breakdown</h3>
                  <p className="text-xs text-slate-500">Real-time candidate demographic tracking requested by HR for talent pool diversity compliance</p>
                </div>
              </div>
              <div className="flex items-center gap-2">
                <span className="text-xs font-bold text-pink-700 bg-pink-50 border border-pink-200 px-3 py-1 rounded-full">
                  👩 Female: {femalePct}%
                </span>
                <span className="text-xs font-bold text-blue-700 bg-blue-50 border border-blue-200 px-3 py-1 rounded-full">
                  👨 Male: {malePct}%
                </span>
              </div>
            </div>

            {/* Multi-segment stacked progress bar */}
            <div className="w-full bg-slate-100 h-3.5 rounded-full overflow-hidden flex shadow-inner mb-6">
              {male > 0 && (
                <div
                  style={{ width: `${(male / total) * 100}%` }}
                  className="bg-blue-600 hover:bg-blue-700 transition"
                  title={`Male: ${male} (${malePct}%)`}
                />
              )}
              {female > 0 && (
                <div
                  style={{ width: `${(female / total) * 100}%` }}
                  className="bg-pink-500 hover:bg-pink-600 transition"
                  title={`Female: ${female} (${femalePct}%)`}
                />
              )}
              {other > 0 && (
                <div
                  style={{ width: `${(other / total) * 100}%` }}
                  className="bg-purple-500 hover:bg-purple-600 transition"
                  title={`Other: ${other} (${otherPct}%)`}
                />
              )}
              {unspecified > 0 && (
                <div
                  style={{ width: `${(unspecified / total) * 100}%` }}
                  className="bg-slate-300 hover:bg-slate-400 transition"
                  title={`Unspecified: ${unspecified} (${unspecPct}%)`}
                />
              )}
            </div>

            {/* Breakdown Mini-Cards */}
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
              <div className="p-4 rounded-2xl bg-blue-50/60 border border-blue-100 flex items-center justify-between">
                <div>
                  <div className="text-[11px] font-bold text-blue-800 uppercase tracking-wider flex items-center gap-1.5">
                    <span>👨</span>
                    <span>Male Candidates</span>
                  </div>
                  <div className="text-2xl font-black text-blue-950 mt-1">{male}</div>
                </div>
                <div className="text-right">
                  <span className="text-xs font-bold text-blue-600 bg-white px-2 py-1 rounded-lg shadow-sm border border-blue-100">
                    {malePct}%
                  </span>
                </div>
              </div>

              <div className="p-4 rounded-2xl bg-pink-50/60 border border-pink-100 flex items-center justify-between">
                <div>
                  <div className="text-[11px] font-bold text-pink-800 uppercase tracking-wider flex items-center gap-1.5">
                    <span>👩</span>
                    <span>Female Candidates</span>
                  </div>
                  <div className="text-2xl font-black text-pink-950 mt-1">{female}</div>
                </div>
                <div className="text-right">
                  <span className="text-xs font-bold text-pink-600 bg-white px-2 py-1 rounded-lg shadow-sm border border-pink-100">
                    {femalePct}%
                  </span>
                </div>
              </div>

              <div className="p-4 rounded-2xl bg-purple-50/60 border border-purple-100 flex items-center justify-between">
                <div>
                  <div className="text-[11px] font-bold text-purple-800 uppercase tracking-wider flex items-center gap-1.5">
                    <span>⚧</span>
                    <span>Other / Non-Binary</span>
                  </div>
                  <div className="text-2xl font-black text-purple-950 mt-1">{other}</div>
                </div>
                <div className="text-right">
                  <span className="text-xs font-bold text-purple-600 bg-white px-2 py-1 rounded-lg shadow-sm border border-purple-100">
                    {otherPct}%
                  </span>
                </div>
              </div>

              <div className="p-4 rounded-2xl bg-slate-50 border border-slate-200/80 flex items-center justify-between">
                <div>
                  <div className="text-[11px] font-bold text-slate-600 uppercase tracking-wider flex items-center gap-1.5">
                    <span>👤</span>
                    <span>Unspecified</span>
                  </div>
                  <div className="text-2xl font-black text-slate-900 mt-1">{unspecified}</div>
                </div>
                <div className="text-right">
                  <span className="text-xs font-bold text-slate-500 bg-white px-2 py-1 rounded-lg shadow-sm border border-slate-200">
                    {unspecPct}%
                  </span>
                </div>
              </div>
            </div>
          </div>
        );
      })()}

      {/* Recent Interviewer Evaluations & Feedback */}
      {(() => {
        const evaluatedCandidates = candidates.filter(cand => 
          (cand.interviews || []).some(i => i.evaluatedAt || i.technicalRating || (i.feedback && i.result !== 'PENDING'))
        );

        return (
          <div className="bg-white rounded-3xl border border-slate-200/80 shadow-sm p-6 space-y-4">
            <div className="flex items-center justify-between flex-wrap gap-2">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-2xl bg-gradient-to-br from-amber-500 to-orange-600 flex items-center justify-center text-white text-lg shadow-md shadow-amber-500/20">
                  ⭐
                </div>
                <div>
                  <h3 className="font-extrabold text-base text-slate-900 tracking-tight">Interviewer Feedback & Candidate Ratings</h3>
                  <p className="text-xs text-slate-500">Live evaluations submitted by interviewers across Technical, Communication, Confidence, and Demeanor</p>
                </div>
              </div>
              <span className="text-xs font-bold text-amber-800 bg-amber-50 border border-amber-200 px-3 py-1 rounded-full">
                {evaluatedCandidates.length} Evaluated {evaluatedCandidates.length === 1 ? 'Candidate' : 'Candidates'}
              </span>
            </div>

            {evaluatedCandidates.length === 0 ? (
              <div className="py-8 text-center bg-slate-50 rounded-2xl border border-dashed border-slate-200 text-xs text-slate-400">
                No interviewer evaluations recorded yet. When interviewers complete interviews and submit feedback, their ratings and comments will appear here.
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {evaluatedCandidates.slice(0, 6).map(cand => {
                  const latestEval = (cand.interviews || [])
                    .filter(i => i.evaluatedAt || i.technicalRating || (i.feedback && i.result !== 'PENDING'))
                    .slice(-1)[0];
                  if (!latestEval) return null;

                  const isGood = latestEval.result === 'SELECTED';
                  const overall = latestEval.overallRating || 
                    (latestEval.technicalRating ? ((latestEval.technicalRating + latestEval.communicationRating + latestEval.confidenceRating + latestEval.bodyLanguageRating) / 4).toFixed(1) : null);

                  return (
                    <div
                      key={cand.id}
                      onClick={() => onSelectCandidate(cand)}
                      className="p-4 rounded-2xl bg-gradient-to-br from-slate-50 to-white border border-slate-200 hover:border-blue-400 hover:shadow-md transition cursor-pointer space-y-3"
                    >
                      <div className="flex items-start justify-between">
                        <div>
                          <div className="flex items-center gap-2">
                            <span className="font-bold text-slate-900 text-sm">{cand.fullName}</span>
                            {isGood ? (
                              <span className="text-[10px] font-black uppercase text-emerald-800 bg-emerald-100 border border-emerald-300 px-2 py-0.5 rounded-full">
                                ✓ Good (Passed)
                              </span>
                            ) : (
                              <span className="text-[10px] font-black uppercase text-rose-800 bg-rose-100 border border-rose-300 px-2 py-0.5 rounded-full">
                                ✕ Bad (Not Selected)
                              </span>
                            )}
                          </div>
                          <div className="text-xs text-slate-500 mt-0.5">
                            {cand.job?.jobTitle || cand.jobTitle || 'Role'} • <strong className="text-indigo-600">{latestEval.roundDisplayName || formatRoundName(latestEval.round)}</strong>
                          </div>
                        </div>
                        {overall && (
                          <div className="text-right">
                            <div className="text-xs font-black text-amber-600 bg-amber-50 border border-amber-200 px-2.5 py-1 rounded-xl flex items-center gap-1 shadow-sm">
                              <span>⭐</span>
                              <span>{overall} / 5.0</span>
                            </div>
                          </div>
                        )}
                      </div>

                      {/* 4 Parameters Mini-Grid */}
                      {latestEval.technicalRating && (
                        <div className="grid grid-cols-4 gap-1.5 text-[11px] bg-slate-100/70 p-2 rounded-xl">
                          <div className="text-center">
                            <span className="text-slate-400 block text-[9px] uppercase font-bold">Tech</span>
                            <span className="font-extrabold text-slate-800">⭐ {latestEval.technicalRating}/5</span>
                          </div>
                          <div className="text-center border-l border-slate-200">
                            <span className="text-slate-400 block text-[9px] uppercase font-bold">Comm</span>
                            <span className="font-extrabold text-slate-800">⭐ {latestEval.communicationRating}/5</span>
                          </div>
                          <div className="text-center border-l border-slate-200">
                            <span className="text-slate-400 block text-[9px] uppercase font-bold">Conf</span>
                            <span className="font-extrabold text-slate-800">⭐ {latestEval.confidenceRating}/5</span>
                          </div>
                          <div className="text-center border-l border-slate-200">
                            <span className="text-slate-400 block text-[9px] uppercase font-bold">Body</span>
                            <span className="font-extrabold text-slate-800">⭐ {latestEval.bodyLanguageRating}/5</span>
                          </div>
                        </div>
                      )}

                      {/* Evaluator Comment Snippet */}
                      {(latestEval.comments || latestEval.feedback) && (
                        <div className="text-xs text-slate-600 bg-amber-50/40 p-2.5 rounded-xl border border-amber-100 line-clamp-2 italic">
                          "{latestEval.comments || latestEval.feedback}"
                        </div>
                      )}

                      <div className="flex items-center justify-between text-[11px] text-slate-400 pt-1 border-t border-slate-100">
                        <span>Evaluated by <strong className="text-slate-600">{latestEval.interviewerName || 'Panelist'}</strong></span>
                        <span className="font-bold text-blue-600 flex items-center gap-1">
                          <span>View Scorecard</span>
                          <span>&rarr;</span>
                        </span>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        );
      })()}

      <div className="bg-white rounded-2xl border border-slate-200/80 shadow-sm overflow-hidden">
        <div className="p-6 border-b border-slate-100 flex items-center justify-between">
          <div>
            <h3 className="font-bold text-lg text-slate-900">Recent Applications & ATS Decisions</h3>
            <p className="text-xs text-slate-500 mt-0.5">Click any applicant to inspect ATS score breakdown and dispatched email history</p>
          </div>
          <span className="text-xs font-semibold text-slate-500 bg-slate-100 px-3 py-1.5 rounded-lg">
            Total: {candidates.length}
          </span>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse text-sm">
            <thead>
              <tr className="bg-slate-50/75 text-slate-500 text-xs font-semibold border-b border-slate-200">
                <th className="py-3 px-6">Candidate</th>
                <th className="py-3 px-6">Position</th>
                <th className="py-3 px-6">ATS Score</th>
                <th className="py-3 px-6">Status</th>
                <th className="py-3 px-6">Applied Date</th>
                <th className="py-3 px-6 text-right">Action</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {candidates.slice(0, 8).map(c => (
                <tr
                  key={c.id}
                  onClick={() => onSelectCandidate(c)}
                  className="hover:bg-blue-50/50 cursor-pointer transition"
                >
                  <td className="py-4 px-6">
                    <div className="flex items-center gap-2">
                      <span className="font-semibold text-slate-900">{c.fullName || 'Applicant'}</span>
                      {renderGenderBadge(c.gender)}
                    </div>
                    <div className="text-xs text-slate-500">{c.email || c.senderEmail || c.resumeEmail || 'No email'}</div>
                  </td>
                  <td className="py-4 px-6 text-slate-700 font-medium">
                    {c.job?.jobTitle || c.jobTitle || 'General Application'}
                  </td>
                  <td className="py-4 px-6">
                    {renderScoreBadge(c.finalAtsScore ?? c.atsScore ?? c.atsResult?.finalScore)}
                  </td>
                  <td className="py-4 px-6">
                    {renderStatusBadge(c.status)}
                  </td>
                  <td className="py-4 px-6 text-xs text-slate-500">
                    {c.appliedAt ? new Date(c.appliedAt).toLocaleDateString() : 'Recent'}
                  </td>
                  <td className="py-4 px-6 text-right">
                    <div className="flex items-center justify-end gap-2">
                      <span className="text-xs font-semibold text-blue-600 hover:text-blue-800">
                        Inspect &rarr;
                      </span>
                      {canScheduleInterview(c) && (
                        <button
                          onClick={(e) => { e.stopPropagation(); onOpenSchedule(c); }}
                          className="px-2 py-1 rounded-lg bg-blue-50 hover:bg-blue-100 text-blue-700 text-xs font-bold border border-blue-200 shadow-sm"
                        >
                          📅 Schedule
                        </button>
                      )}
                      {canRecordDecision(c) && (
                        <button
                          onClick={(e) => { e.stopPropagation(); onOpenDecision(c); }}
                          className="px-2 py-1 rounded-lg bg-indigo-50 hover:bg-indigo-100 text-indigo-700 text-xs font-bold border border-indigo-200 shadow-sm"
                        >
                          📝 Decision
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

function JobsTab({ jobs, onOpenCreate, onRefresh }) {
  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl font-black text-slate-900 tracking-tight">Active Job Openings</h2>
          <p className="text-xs text-slate-500 mt-1">Open positions receiving candidate resumes and LinkedIn email applications</p>
        </div>
        <div className="flex items-center gap-3">
          <button
            onClick={onRefresh}
            className="px-4 py-2 rounded-xl bg-white border border-slate-200 text-slate-700 text-sm font-semibold hover:bg-slate-50 shadow-sm"
          >
            🔄 Refresh
          </button>
          <button
            onClick={onOpenCreate}
            className="px-5 py-2.5 rounded-xl bg-blue-600 hover:bg-blue-500 text-white text-sm font-semibold shadow-lg shadow-blue-600/25 flex items-center gap-2"
          >
            <span>➕</span>
            <span>Create New Job</span>
          </button>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
        {jobs.map(job => (
          <div key={job.id} className="bg-white rounded-2xl p-6 border border-slate-200 shadow-sm hover:shadow-md transition">
            <div className="flex items-start justify-between mb-3">
              <div>
                <span className="text-xs font-semibold text-blue-600 bg-blue-50 border border-blue-200/60 px-2.5 py-0.5 rounded-full uppercase">
                  {job.department || 'Engineering'}
                </span>
                <h3 className="text-lg font-bold text-slate-900 mt-2">{job.jobTitle}</h3>
                <div className="text-xs text-slate-500 mt-0.5">
                  📍 {job.location || 'Remote'} &bull; ⏱️ {job.employmentType || 'FULL_TIME'} &bull; Min Exp: {job.minimumExperience} yrs
                </div>
              </div>
              <span className={`text-xs px-2.5 py-1 rounded-full font-bold ${
                job.status === 'ACTIVE' ? 'bg-emerald-100 text-emerald-800' : 'bg-slate-100 text-slate-700'
              }`}>
                {job.status}
              </span>
            </div>

            <p className="text-xs text-slate-600 line-clamp-2 my-3">{job.description}</p>

            <div className="mt-4">
              <div className="text-[11px] uppercase tracking-wider font-bold text-slate-400 mb-1.5">Required Skills (80% Weight)</div>
              <div className="flex flex-wrap gap-1.5">
                {(job.requiredSkills || []).map((sk, idx) => (
                  <span key={idx} className="text-xs bg-slate-100 text-slate-800 font-medium px-2 py-0.5 rounded-md border border-slate-200">
                    {sk}
                  </span>
                ))}
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

function CandidatesTab({ candidates, jobs, onSelectCandidate, onOpenSchedule, onOpenDecision, onInterviewCandidate, onRefresh }) {
  const [filterJob, setFilterJob] = useState('ALL');
  const [filterStatus, setFilterStatus] = useState('ALL');
  const [filterGender, setFilterGender] = useState('ALL');
  const [searchTerm, setSearchTerm] = useState('');

  const filtered = useMemo(() => {
    return candidates.filter(c => {
      const matchJob = filterJob === 'ALL' || c.job?.id === filterJob || c.jobId === filterJob;
      const matchStatus = filterStatus === 'ALL' || c.status === filterStatus;
      const matchGender = filterGender === 'ALL' || c.gender === filterGender;
      const search = searchTerm.toLowerCase();
      const matchSearch = !search ||
        (c.fullName && c.fullName.toLowerCase().includes(search)) ||
        (c.email && c.email.toLowerCase().includes(search)) ||
        (c.senderEmail && c.senderEmail.toLowerCase().includes(search));
      return matchJob && matchStatus && matchGender && matchSearch;
    });
  }, [candidates, filterJob, filterStatus, filterGender, searchTerm]);

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl font-black text-slate-900 tracking-tight">Candidate Pipeline</h2>
          <p className="text-xs text-slate-500 mt-1">Review applicant screening decisions, ATS breakdown, and stage progression</p>
        </div>
        <button
          onClick={onRefresh}
          className="px-4 py-2 rounded-xl bg-white border border-slate-200 text-slate-700 text-sm font-semibold hover:bg-slate-50 shadow-sm"
        >
          🔄 Refresh List
        </button>
      </div>

      <div className="bg-white rounded-2xl p-4 border border-slate-200 shadow-sm flex flex-wrap items-center gap-3">
        <div className="flex-1 min-w-[200px]">
          <input
            type="text"
            placeholder="Search by candidate name or email..."
            value={searchTerm}
            onChange={e => setSearchTerm(e.target.value)}
            className="w-full text-sm px-3.5 py-2 border border-slate-200 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>

        <select
          value={filterJob}
          onChange={e => setFilterJob(e.target.value)}
          className="text-sm px-3.5 py-2 border border-slate-200 rounded-xl bg-white"
        >
          <option value="ALL">All Jobs ({jobs.length})</option>
          {jobs.map(j => <option key={j.id} value={j.id}>{j.jobTitle}</option>)}
        </select>

        <select
          value={filterStatus}
          onChange={e => setFilterStatus(e.target.value)}
          className="text-sm px-3.5 py-2 border border-slate-200 rounded-xl bg-white"
        >
          <option value="ALL">All Statuses</option>
          <option value="SHORTLISTED">SHORTLISTED (ATS &ge; 80)</option>
          <option value="L1_SCHEDULED">L1 SCHEDULED</option>
          <option value="L1_PASSED">L1 PASSED</option>
          <option value="L2_SCHEDULED">L2 SCHEDULED</option>
          <option value="L2_PASSED">L2 PASSED</option>
          <option value="MANAGERIAL_SCHEDULED">MANAGERIAL SCHEDULED</option>
          <option value="MANAGERIAL_PASSED">MANAGERIAL PASSED</option>
          <option value="HR_SCHEDULED">HR ROUND SCHEDULED</option>
          <option value="SELECTED">SELECTED / HIRED</option>
          <option value="NOT_SELECTED">NOT SELECTED</option>
          <option value="REVIEW_REQUIRED">REVIEW REQUIRED (60-79)</option>
          <option value="REJECTED">REJECTED (&lt; 60)</option>
        </select>

        <select
          value={filterGender}
          onChange={e => setFilterGender(e.target.value)}
          className="text-sm px-3.5 py-2 border border-slate-200 rounded-xl bg-white"
        >
          <option value="ALL">All Genders</option>
          <option value="MALE">👨 Male</option>
          <option value="FEMALE">👩 Female</option>
          <option value="OTHER">⚧ Other</option>
          <option value="UNSPECIFIED">👤 Unspecified</option>
        </select>
      </div>

      <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse text-sm">
            <thead>
              <tr className="bg-slate-50 text-slate-500 text-xs font-semibold border-b border-slate-200">
                <th className="py-3 px-6">Candidate</th>
                <th className="py-3 px-6">Position</th>
                <th className="py-3 px-6">ATS Score</th>
                <th className="py-3 px-6">Status</th>
                <th className="py-3 px-6">Resume File</th>
                <th className="py-3 px-6 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {filtered.map(c => (
                <tr key={c.id} className="hover:bg-blue-50/40 transition">
                  <td className="py-4 px-6 cursor-pointer" onClick={() => onSelectCandidate(c)}>
                    <div className="flex items-center gap-2">
                      <span className="font-bold text-slate-900">{c.fullName || 'Applicant'}</span>
                      {renderGenderBadge(c.gender)}
                    </div>
                    <div className="text-xs text-slate-500">{c.email || c.senderEmail || c.resumeEmail || 'No email'}</div>
                  </td>
                  <td className="py-4 px-6 text-slate-700 font-medium">{c.job?.jobTitle || c.jobTitle || 'General Application'}</td>
                  <td className="py-4 px-6">{renderScoreBadge(c.finalAtsScore ?? c.atsScore ?? c.atsResult?.finalScore)}</td>
                  <td className="py-4 px-6">{renderStatusBadge(c.status)}</td>
                  <td className="py-4 px-6 text-xs text-slate-600 font-mono">{c.resumeFileName || 'None'}</td>
                  <td className="py-4 px-6 text-right">
                    <div className="flex items-center justify-end gap-2">
                      <button
                        onClick={() => onSelectCandidate(c)}
                        className="px-3 py-1.5 rounded-lg bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-semibold"
                      >
                        Profile
                      </button>
                      {canScheduleInterview(c) && (
                        <button
                          onClick={() => onOpenSchedule ? onOpenSchedule(c) : onInterviewCandidate(c)}
                          className="px-3 py-1.5 rounded-lg bg-blue-50 hover:bg-blue-100 text-blue-700 text-xs font-bold border border-blue-200 flex items-center gap-1 shadow-sm"
                        >
                          <span>📅</span>
                          <span>Schedule {formatRoundName(getNextPendingRound(c)).split(':')[0]}</span>
                        </button>
                      )}
                      {canRecordDecision(c) && (
                        <div className="flex items-center gap-1.5">
                          <button
                            onClick={() => onOpenDecision ? onOpenDecision(c) : onInterviewCandidate(c)}
                            className="px-3 py-1.5 rounded-lg bg-indigo-50 hover:bg-indigo-100 text-indigo-700 text-xs font-bold border border-indigo-200 shadow-sm"
                          >
                            📝 Decision
                          </button>
                          {(() => {
                            const active = getActiveScheduledInterview(c);
                            return active?.meetingLink ? (
                              <a
                                href={active.meetingLink}
                                target="_blank"
                                rel="noopener noreferrer"
                                title="Join Google Meet"
                                className="p-1.5 rounded-lg bg-emerald-50 hover:bg-emerald-100 text-emerald-700 border border-emerald-200 text-xs"
                              >
                                🎥
                              </a>
                            ) : null;
                          })()}
                        </div>
                      )}
                      {c.status === 'SELECTED' && (
                        <span className="text-[11px] font-bold text-emerald-700 bg-emerald-50 border border-emerald-200 px-2 py-0.5 rounded-md">
                          🎉 Hired
                        </span>
                      )}
                      {c.status === 'NOT_SELECTED' && (
                        <span className="text-[11px] font-bold text-rose-700 bg-rose-50 border border-rose-200 px-2 py-0.5 rounded-md">
                          🚫 Not Selected
                        </span>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

function SimulatorTab({ jobs, auth, onSuccess }) {
  const [senderEmail, setSenderEmail] = useState('jessy.smith.dev@gmail.com');
  const [senderName, setSenderName] = useState('Jessy Smith');
  const [gender, setGender] = useState('AUTO');
  const [subject, setSubject] = useState('Application for Senior Java Spring Boot Developer - Jessy Smith');
  const [body, setBody] = useState('Dear Hiring Team,\n\nI am excited to apply for the Senior Java Spring Boot Developer position. Please find my resume attached.\n\nBest regards,\nJessy Smith');
  const [selectedJobId, setSelectedJobId] = useState(jobs[0]?.id || '');
  const [file, setFile] = useState(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [simulationResult, setSimulationResult] = useState(null);
  const fileInputRef = useRef(null);

  const applyPreset = (key) => {
    if (key === 'valid_shortlisted') {
      setSenderEmail('jessy.smith.dev@gmail.com');
      setSenderName('Jessy Smith');
      setGender('FEMALE');
      setSubject('Application for Senior Java Spring Boot Developer - Jessy Smith');
      setBody('Dear Hiring Team,\n\nI am excited to submit my resume for the Senior Java Spring Boot Developer position.');
    } else if (key === 'missing_subject_body') {
      setSenderEmail('incomplete.applicant@example.com');
      setSenderName('Incomplete Applicant');
      setGender('AUTO');
      setSubject('');
      setBody('');
      setFile(null);
    } else if (key === 'missing_attachment') {
      setSenderEmail('amit.singh@example.com');
      setSenderName('Amit Singh');
      setGender('MALE');
      setSubject('Application for Senior Java Developer');
      setBody('Dear Recruitment Team,\n\nPlease find my application for the Senior Java Developer position.');
      setFile(null);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setIsSubmitting(true);
    setSimulationResult(null);

    try {
      const formData = new FormData();
      formData.append('senderEmail', senderEmail);
      if (senderName) formData.append('senderName', senderName);
      if (gender && gender !== 'AUTO') formData.append('gender', gender);
      if (subject) formData.append('subject', subject);
      if (body) formData.append('body', body);
      if (selectedJobId) formData.append('jobId', selectedJobId);
      if (file) formData.append('file', file);

      const res = await fetch(`${API_BASE}/api/v1/gmail/simulate-inbound`, {
        method: 'POST',
        headers: getAuthHeaders(auth),
        body: formData
      });

      const data = await res.json();
      setSimulationResult(data);
      if (res.ok && data.success && data.data) {
        onSuccess(data.data);
      }
    } catch (err) {
      setSimulationResult({ success: false, message: 'Simulation error: ' + err.message });
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="space-y-8">
      <div>
        <h2 className="text-2xl font-black text-slate-900 tracking-tight">Inbound Email & Resume Simulator</h2>
        <p className="text-xs text-slate-500 mt-1">Test validation of subject, cover note, attachment, and automatic mail shooting</p>
      </div>

      <div className="bg-white rounded-2xl p-5 border border-slate-200 shadow-sm">
        <div className="text-xs font-bold text-slate-400 uppercase tracking-wider mb-3">Quick Demo Presets</div>
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
          <button
            type="button"
            onClick={() => applyPreset('valid_shortlisted')}
            className="text-left p-3 rounded-xl border border-emerald-200 bg-emerald-50/50 hover:bg-emerald-50"
          >
            <div className="text-xs font-bold text-emerald-900">1. Shortlisted (Score &ge; 80)</div>
            <div className="text-[11px] text-emerald-700 mt-1">Jessy Smith (Java & Spring Boot match). Shoots Shortlist email.</div>
          </button>
          <button
            type="button"
            onClick={() => applyPreset('missing_subject_body')}
            className="text-left p-3 rounded-xl border border-rose-200 bg-rose-50/50 hover:bg-rose-50"
          >
            <div className="text-xs font-bold text-rose-900">2. Incomplete: Missing Subject/Body</div>
            <div className="text-[11px] text-rose-700 mt-1">Empty subject & body. Shoots Incomplete Rejection notice.</div>
          </button>
          <button
            type="button"
            onClick={() => applyPreset('missing_attachment')}
            className="text-left p-3 rounded-xl border border-amber-200 bg-amber-50/50 hover:bg-amber-50"
          >
            <div className="text-xs font-bold text-amber-900">3. Incomplete: Missing Resume</div>
            <div className="text-[11px] text-amber-700 mt-1">Valid text but omitted resume attachment.</div>
          </button>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
        <div className="lg:col-span-7 bg-white rounded-3xl p-7 border border-slate-200 shadow-sm">
          <form onSubmit={handleSubmit} className="space-y-5">
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1">Sender Email *</label>
                <input
                  type="email"
                  required
                  value={senderEmail}
                  onChange={e => setSenderEmail(e.target.value)}
                  className="w-full text-sm px-3.5 py-2.5 border border-slate-200 rounded-xl"
                />
              </div>
              <div>
                <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1">Candidate Name</label>
                <input
                  type="text"
                  value={senderName}
                  onChange={e => setSenderName(e.target.value)}
                  className="w-full text-sm px-3.5 py-2.5 border border-slate-200 rounded-xl"
                />
              </div>
            </div>

            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1">Target Job Position</label>
                <select
                  value={selectedJobId}
                  onChange={e => setSelectedJobId(e.target.value)}
                  className="w-full text-sm px-3.5 py-2.5 border border-slate-200 rounded-xl bg-white"
                >
                  {jobs.map(j => <option key={j.id} value={j.id}>{j.jobTitle} ({j.department})</option>)}
                </select>
              </div>
              <div>
                <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1">Gender / Demographic (HR)</label>
                <select
                  value={gender}
                  onChange={e => setGender(e.target.value)}
                  className="w-full text-sm px-3.5 py-2.5 border border-slate-200 rounded-xl bg-white"
                >
                  <option value="AUTO">✨ Auto-Detect (from Name/Resume)</option>
                  <option value="MALE">👨 Male</option>
                  <option value="FEMALE">👩 Female</option>
                  <option value="OTHER">⚧ Other</option>
                  <option value="UNSPECIFIED">👤 Unspecified</option>
                </select>
              </div>
            </div>

            <div>
              <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1">Subject Line</label>
              <input
                type="text"
                value={subject}
                onChange={e => setSubject(e.target.value)}
                className="w-full text-sm px-3.5 py-2.5 border border-slate-200 rounded-xl"
              />
            </div>

            <div>
              <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1">Email Body / Cover Note</label>
              <textarea
                rows="4"
                value={body}
                onChange={e => setBody(e.target.value)}
                className="w-full text-sm px-3.5 py-2.5 border border-slate-200 rounded-xl"
              />
            </div>

            <div>
              <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1">Resume File</label>
              <div
                onClick={() => fileInputRef.current?.click()}
                className="border-2 border-dashed rounded-2xl p-6 text-center cursor-pointer border-slate-200 hover:border-blue-400"
              >
                <input
                  type="file"
                  ref={fileInputRef}
                  onChange={e => e.target.files && setFile(e.target.files[0])}
                  className="hidden"
                />
                {file ? (
                  <div className="text-sm font-semibold text-blue-900">📄 {file.name}</div>
                ) : (
                  <div className="text-sm text-slate-600">Click to select PDF resume file</div>
                )}
              </div>
            </div>

            <button
              type="submit"
              disabled={isSubmitting}
              className="w-full py-3.5 px-6 rounded-2xl bg-blue-600 hover:bg-blue-500 text-white font-bold text-sm shadow-xl shadow-blue-500/25"
            >
              {isSubmitting ? 'Simulating & Shooting Mail...' : 'Simulate Inbound & Shoot Email'}
            </button>
          </form>
        </div>

        <div className="lg:col-span-5 bg-white rounded-3xl p-6 border border-slate-200 shadow-sm min-h-[400px]">
          <h3 className="font-bold text-base text-slate-900 mb-4 pb-3 border-b border-slate-100">Live Outcome</h3>
          {simulationResult ? (
            <div className="space-y-4">
              <div className="p-4 rounded-2xl bg-slate-50 border text-sm">
                <div className="font-bold">Status: {simulationResult.data?.status || 'PROCESSED'}</div>
                <p className="text-xs text-slate-600 mt-1">{simulationResult.message}</p>
              </div>
              {simulationResult.data?.atsResult && (
                <div className="text-xs p-3 bg-slate-50 rounded-xl">
                  Score: <strong>{simulationResult.data.atsResult.finalScore} / 100</strong>
                </div>
              )}
            </div>
          ) : (
            <div className="text-center py-20 text-slate-400 text-sm">Submit simulation above to inspect outcome</div>
          )}
        </div>
      </div>
    </div>
  );
}

function GmailTab({ gmailStatus, auth, onRefresh, showToast }) {
  return (
    <div className="space-y-6 max-w-4xl mx-auto">
      <div>
        <h2 className="text-2xl font-black text-slate-900 tracking-tight">Gmail Integration</h2>
        <p className="text-xs text-slate-500 mt-1">Recruitment Gmail connection and polling status</p>
      </div>

      <div className="bg-white rounded-3xl p-7 border border-slate-200 shadow-sm space-y-4">
        <div className="flex items-center gap-3">
          <span className="w-3 h-3 rounded-full bg-emerald-500 animate-pulse"></span>
          <span className="font-bold text-emerald-800">Connected & Authorized via OAuth 2.0</span>
        </div>
        <div className="text-xs text-slate-600">Client ID: {gmailStatus?.clientId}</div>
        <div className="text-xs text-slate-600">Recruitment Email: {gmailStatus?.recruitmentEmail}</div>
      </div>
    </div>
  );
}

// ==========================================
// INTERVIEWERS DIRECTORY & PANEL MANAGEMENT
// ==========================================
export function InterviewersManagementView({ auth, interviewers, onRefreshInterviewers, showToast }) {
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [showPasswordModal, setShowPasswordModal] = useState(null);
  const [newPassword, setNewPassword] = useState('');
  const [submittingPassword, setSubmittingPassword] = useState(false);
  const [createForm, setCreateForm] = useState({
    fullName: '',
    email: '',
    password: '',
    designation: '',
    department: 'Engineering',
    phoneNumber: ''
  });
  const [submittingCreate, setSubmittingCreate] = useState(false);
  const [lastCreatedAccount, setLastCreatedAccount] = useState(null);

  const handleCreateInterviewer = async (e) => {
    if (e) e.preventDefault();
    if (!createForm.fullName.trim() || !createForm.email.trim() || !createForm.password.trim()) {
      showToast('Please fill all required fields (Name, Email, Password)', 'error');
      return;
    }
    setSubmittingCreate(true);
    try {
      const res = await fetch(`${API_BASE}/api/v1/hr/interviewers`, {
        method: 'POST',
        headers: {
          ...getAuthHeaders(auth),
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(createForm)
      });
      const json = await res.json();
      if (res.ok && json.success) {
        setLastCreatedAccount({
          fullName: createForm.fullName,
          email: createForm.email,
          password: createForm.password,
          designation: createForm.designation
        });
        showToast('Interviewer account created successfully!', 'success');
        setCreateForm({
          fullName: '',
          email: '',
          password: '',
          designation: '',
          department: 'Engineering',
          phoneNumber: ''
        });
        setShowCreateModal(false);
        if (onRefreshInterviewers) onRefreshInterviewers();
      } else {
        showToast(json.message || 'Failed to create interviewer account', 'error');
      }
    } catch (err) {
      showToast('Error creating interviewer: ' + err.message, 'error');
    } finally {
      setSubmittingCreate(false);
    }
  };

  const handleToggleStatus = async (id) => {
    try {
      const res = await fetch(`${API_BASE}/api/v1/hr/interviewers/${id}/toggle-status`, {
        method: 'PATCH',
        headers: getAuthHeaders(auth)
      });
      const json = await res.json();
      if (res.ok && json.success) {
        showToast('Interviewer status toggled successfully', 'success');
        if (onRefreshInterviewers) onRefreshInterviewers();
      } else {
        showToast(json.message || 'Failed to toggle status', 'error');
      }
    } catch (err) {
      showToast('Error toggling status: ' + err.message, 'error');
    }
  };

  const handleUpdatePassword = async (e) => {
    if (e) e.preventDefault();
    if (!newPassword || newPassword.length < 6) {
      showToast('Password must be at least 6 characters', 'error');
      return;
    }
    setSubmittingPassword(true);
    try {
      const res = await fetch(`${API_BASE}/api/v1/hr/interviewers/${showPasswordModal.id}`, {
        method: 'PUT',
        headers: {
          ...getAuthHeaders(auth),
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ newPassword: newPassword })
      });
      const json = await res.json();
      if (res.ok && json.success) {
        showToast(`Password updated for ${showPasswordModal.fullName}!`, 'success');
        setShowPasswordModal(null);
        setNewPassword('');
      } else {
        showToast(json.message || 'Failed to update password', 'error');
      }
    } catch (err) {
      showToast('Error updating password: ' + err.message, 'error');
    } finally {
      setSubmittingPassword(false);
    }
  };

  const totalAssigned = (interviewers || []).reduce((acc, i) => acc + (i.assignedInterviewsCount || 0), 0);
  const totalCompleted = (interviewers || []).reduce((acc, i) => acc + (i.completedEvaluationsCount || 0), 0);
  const activeCount = (interviewers || []).filter(i => i.active).length;

  return (
    <div className="space-y-6">
      {/* Header Banner */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <span className="text-2xl">👨‍💼</span>
            <h2 className="text-2xl font-black text-slate-900 tracking-tight">Interviewer Directory & Panel Management</h2>
          </div>
          <p className="text-xs sm:text-sm text-slate-500 mt-1">
            Create and manage interviewer accounts. Panelists can log in to the Interviewer Portal with their credentials to evaluate assigned candidates across 4 parameters.
          </p>
        </div>
        <div className="flex items-center gap-3">
          <button
            type="button"
            onClick={onRefreshInterviewers}
            className="p-2.5 rounded-xl border border-slate-300 hover:bg-slate-100 text-slate-700 transition"
            title="Refresh Interviewers List"
          >
            🔄
          </button>
          <button
            type="button"
            onClick={() => setShowCreateModal(true)}
            className="px-5 py-2.5 rounded-xl bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-500 hover:to-indigo-500 text-white font-bold text-xs sm:text-sm shadow-md shadow-blue-500/20 transition flex items-center gap-2"
          >
            <span>➕</span>
            <span>Create Interviewer Account</span>
          </button>
        </div>
      </div>

      {/* Last Created Account Success Notice */}
      {lastCreatedAccount && (
        <div className="p-4 rounded-2xl bg-emerald-50 border border-emerald-300 text-emerald-950 flex items-start justify-between gap-3 shadow-sm">
          <div className="space-y-1">
            <div className="font-extrabold text-sm flex items-center gap-2 text-emerald-900">
              <span>✅</span>
              <span>Interviewer Account Created Successfully!</span>
            </div>
            <div className="text-xs text-emerald-800">
              Share these login credentials with the interviewer so they can log in to the <strong>Interviewer Portal</strong>:
            </div>
            <div className="inline-flex flex-wrap items-center gap-4 mt-2 p-2.5 bg-white/80 rounded-xl border border-emerald-200 font-mono text-xs text-slate-900">
              <div><strong>Name:</strong> {lastCreatedAccount.fullName}</div>
              <div><strong>Email / Username:</strong> <span className="text-blue-700 font-bold">{lastCreatedAccount.email}</span></div>
              <div><strong>Password:</strong> <span className="text-emerald-700 font-bold">{lastCreatedAccount.password}</span></div>
              <div><strong>Role:</strong> Panel Interviewer</div>
            </div>
          </div>
          <button onClick={() => setLastCreatedAccount(null)} className="text-emerald-700 hover:text-emerald-900 font-bold text-lg">&times;</button>
        </div>
      )}

      {/* Metrics Overview */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
        <div className="bg-white p-5 rounded-2xl border border-slate-200/80 shadow-sm">
          <div className="text-xs font-bold text-slate-500 uppercase tracking-wider">Total Interviewers</div>
          <div className="text-2xl font-black text-slate-900 mt-1">{(interviewers || []).length}</div>
        </div>
        <div className="bg-white p-5 rounded-2xl border border-slate-200/80 shadow-sm">
          <div className="text-xs font-bold text-emerald-600 uppercase tracking-wider">Active Panelists</div>
          <div className="text-2xl font-black text-emerald-700 mt-1">{activeCount}</div>
        </div>
        <div className="bg-white p-5 rounded-2xl border border-slate-200/80 shadow-sm">
          <div className="text-xs font-bold text-blue-600 uppercase tracking-wider">Total Assigned Rounds</div>
          <div className="text-2xl font-black text-blue-700 mt-1">{totalAssigned}</div>
        </div>
        <div className="bg-white p-5 rounded-2xl border border-slate-200/80 shadow-sm">
          <div className="text-xs font-bold text-amber-600 uppercase tracking-wider">Completed Evaluations</div>
          <div className="text-2xl font-black text-amber-700 mt-1">{totalCompleted}</div>
        </div>
      </div>

      {/* Interviewers Table */}
      <div className="bg-white rounded-2xl border border-slate-200/80 shadow-sm overflow-hidden">
        <div className="px-6 py-4 border-b border-slate-200 flex items-center justify-between">
          <h3 className="font-extrabold text-slate-900 text-sm uppercase tracking-wider">Registered Panelists</h3>
          <span className="text-xs text-slate-500 font-medium">Panelists can sign in to evaluate candidates</span>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs sm:text-sm">
            <thead className="bg-slate-50 text-slate-600 font-bold uppercase text-[11px] tracking-wider border-b border-slate-200">
              <tr>
                <th className="py-3 px-6">Interviewer</th>
                <th className="py-3 px-6">Work Email (Login ID)</th>
                <th className="py-3 px-6">Department</th>
                <th className="py-3 px-6">Status</th>
                <th className="py-3 px-6 text-center">Assigned</th>
                <th className="py-3 px-6 text-center">Evaluated</th>
                <th className="py-3 px-6 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 text-slate-800">
              {(!interviewers || interviewers.length === 0) ? (
                <tr>
                  <td colSpan="7" className="py-12 text-center text-slate-400">
                    No interviewers registered yet. Click "Create Interviewer Account" to add the first panelist.
                  </td>
                </tr>
              ) : (
                interviewers.map(i => (
                  <tr key={i.id} className="hover:bg-slate-50/80 transition">
                    <td className="py-4 px-6">
                      <div className="flex items-center gap-3">
                        <div className="w-9 h-9 rounded-xl bg-blue-100 text-blue-800 font-black flex items-center justify-center text-xs uppercase shadow-sm">
                          {i.fullName ? i.fullName.substring(0, 2) : 'IN'}
                        </div>
                        <div>
                          <div className="font-bold text-slate-900">{i.fullName}</div>
                          <div className="text-[11px] text-slate-500">{i.designation || 'Interview Panelist'}</div>
                        </div>
                      </div>
                    </td>
                    <td className="py-4 px-6 font-mono text-xs font-semibold text-slate-700">
                      {i.email}
                    </td>
                    <td className="py-4 px-6 text-xs text-slate-600">
                      {i.department || 'Engineering'}
                    </td>
                    <td className="py-4 px-6">
                      <span className={`inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-[11px] font-bold ${
                        i.active
                          ? 'bg-emerald-100 text-emerald-800 border border-emerald-200'
                          : 'bg-slate-100 text-slate-600 border border-slate-200'
                      }`}>
                        <span className={`w-1.5 h-1.5 rounded-full ${i.active ? 'bg-emerald-500' : 'bg-slate-400'}`}></span>
                        <span>{i.active ? 'Active' : 'Inactive'}</span>
                      </span>
                    </td>
                    <td className="py-4 px-6 text-center font-bold text-slate-900">
                      {i.assignedInterviewsCount || 0}
                    </td>
                    <td className="py-4 px-6 text-center font-bold text-emerald-700">
                      {i.completedEvaluationsCount || 0}
                    </td>
                    <td className="py-4 px-6 text-right">
                      <div className="flex items-center justify-end gap-2">
                        <button
                          type="button"
                          onClick={() => {
                            setShowPasswordModal(i);
                            setNewPassword('');
                          }}
                          className="px-2.5 py-1 text-xs rounded-lg border border-slate-300 hover:bg-slate-100 text-slate-700 font-semibold transition"
                          title="Reset Login Password"
                        >
                          Reset Password
                        </button>
                        <button
                          type="button"
                          onClick={() => handleToggleStatus(i.id)}
                          className={`px-2.5 py-1 text-xs rounded-lg font-semibold transition ${
                            i.active
                              ? 'bg-rose-50 text-rose-700 hover:bg-rose-100 border border-rose-200'
                              : 'bg-emerald-50 text-emerald-700 hover:bg-emerald-100 border border-emerald-200'
                          }`}
                        >
                          {i.active ? 'Deactivate' : 'Activate'}
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Create Interviewer Modal */}
      {showCreateModal && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-3 sm:p-4 overflow-y-auto">
          <div className="bg-white rounded-3xl max-w-lg w-full shadow-2xl overflow-hidden animate-scale-up flex flex-col max-h-[90vh] my-auto">
            <div className="p-5 sm:p-6 bg-slate-900 text-white flex items-center justify-between shrink-0">
              <div>
                <span className="text-xs font-bold text-blue-400 uppercase tracking-wider">HR Panelist Creation</span>
                <h3 className="font-black text-lg mt-0.5">Create New Interviewer Account</h3>
                <p className="text-xs text-slate-400 mt-0.5">The interviewer will use these credentials to log in and evaluate candidates</p>
              </div>
              <button onClick={() => setShowCreateModal(false)} className="text-slate-400 hover:text-white text-xl font-bold p-1">&times;</button>
            </div>

            <form onSubmit={handleCreateInterviewer} className="flex flex-col flex-1 overflow-hidden min-h-0">
              <div className="p-5 sm:p-6 space-y-4 overflow-y-auto custom-scrollbar flex-1">
                <div>
                  <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1">
                    Full Name *
                  </label>
                  <input
                    type="text"
                    required
                    value={createForm.fullName}
                    onChange={e => setCreateForm({ ...createForm, fullName: e.target.value })}
                    placeholder="e.g. Arun Patel"
                    className="w-full text-sm px-3.5 py-2.5 border border-slate-200 rounded-xl focus:ring-2 focus:ring-blue-500 focus:outline-none"
                  />
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1">
                    Work Email (Login Username) *
                  </label>
                  <input
                    type="email"
                    required
                    value={createForm.email}
                    onChange={e => setCreateForm({ ...createForm, email: e.target.value })}
                    placeholder="e.g. arun.patel@sanvii.com"
                    className="w-full text-sm px-3.5 py-2.5 border border-slate-200 rounded-xl focus:ring-2 focus:ring-blue-500 focus:outline-none"
                  />
                </div>

                <div>
                  <div className="flex items-center justify-between mb-1">
                    <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider">
                      Initial Login Password *
                    </label>
                    <button
                      type="button"
                      onClick={() => setCreateForm({ ...createForm, password: 'Sanvii@' + Math.floor(1000 + Math.random() * 9000) })}
                      className="text-[11px] text-blue-600 hover:underline font-semibold"
                    >
                      Generate Random
                    </button>
                  </div>
                  <input
                    type="text"
                    required
                    value={createForm.password}
                    onChange={e => setCreateForm({ ...createForm, password: e.target.value })}
                    placeholder="e.g. Sanvii@2026"
                    className="w-full text-sm font-mono px-3.5 py-2.5 border border-slate-200 rounded-xl focus:ring-2 focus:ring-blue-500 focus:outline-none"
                  />
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  <div>
                    <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1">
                      Designation / Title *
                    </label>
                    <input
                      type="text"
                      required
                      value={createForm.designation}
                      onChange={e => setCreateForm({ ...createForm, designation: e.target.value })}
                      placeholder="e.g. Senior Backend Architect"
                      className="w-full text-sm px-3.5 py-2.5 border border-slate-200 rounded-xl focus:ring-2 focus:ring-blue-500 focus:outline-none"
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1">
                      Department
                    </label>
                    <input
                      type="text"
                      value={createForm.department}
                      onChange={e => setCreateForm({ ...createForm, department: e.target.value })}
                      placeholder="e.g. Engineering"
                      className="w-full text-sm px-3.5 py-2.5 border border-slate-200 rounded-xl focus:ring-2 focus:ring-blue-500 focus:outline-none"
                    />
                  </div>
                </div>

                <div>
                  <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1">
                    Phone Number (Optional)
                  </label>
                  <input
                    type="tel"
                    value={createForm.phoneNumber}
                    onChange={e => setCreateForm({ ...createForm, phoneNumber: e.target.value })}
                    placeholder="+91 98765 43210"
                    className="w-full text-sm px-3.5 py-2.5 border border-slate-200 rounded-xl focus:ring-2 focus:ring-blue-500 focus:outline-none"
                  />
                </div>

                <div className="p-3 bg-blue-50 rounded-xl border border-blue-200 text-[11px] text-blue-900">
                  💡 After creation, provide the email & password to the interviewer. They can sign in at the <strong>Interviewer Portal</strong> to view assigned candidates and record evaluations.
                </div>
              </div>

              {/* Fixed Sticky Footer - Always Visible */}
              <div className="p-4 sm:p-5 bg-slate-50 border-t border-slate-200 flex items-center justify-end gap-3 shrink-0 rounded-b-3xl">
                <button
                  type="button"
                  onClick={() => setShowCreateModal(false)}
                  className="px-4 py-2.5 rounded-xl border border-slate-300 text-xs font-bold text-slate-700 hover:bg-slate-100 transition"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submittingCreate}
                  className="px-6 py-2.5 rounded-xl bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-500 hover:to-indigo-500 text-white text-xs font-black shadow-lg shadow-blue-500/25 transition transform active:scale-95 disabled:opacity-50 flex items-center gap-1.5"
                >
                  <span>{submittingCreate ? '⏳' : '➕'}</span>
                  <span>{submittingCreate ? 'Creating Account...' : 'Create Interviewer Account'}</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Reset Password Modal */}
      {showPasswordModal && (
        <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl max-w-md w-full shadow-2xl overflow-hidden animate-scale-up">
            <div className="p-6 bg-slate-900 text-white flex items-center justify-between">
              <div>
                <span className="text-xs font-bold text-amber-400 uppercase tracking-wider">Account Security</span>
                <h3 className="font-black text-lg mt-0.5">Reset Interviewer Password</h3>
                <p className="text-xs text-slate-400">{showPasswordModal.fullName} ({showPasswordModal.email})</p>
              </div>
              <button onClick={() => setShowPasswordModal(null)} className="text-slate-400 hover:text-white text-xl font-bold">&times;</button>
            </div>
            <form onSubmit={handleUpdatePassword} className="p-6 space-y-4">
              <div>
                <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1">
                  New Security Password *
                </label>
                <input
                  type="text"
                  required
                  value={newPassword}
                  onChange={e => setNewPassword(e.target.value)}
                  placeholder="Enter at least 6 characters"
                  className="w-full text-sm font-mono px-3.5 py-2.5 border border-slate-200 rounded-xl focus:ring-2 focus:ring-blue-500 focus:outline-none"
                />
              </div>
              <div className="pt-3 border-t border-slate-200 flex items-center justify-end gap-3">
                <button
                  type="button"
                  onClick={() => setShowPasswordModal(null)}
                  className="px-4 py-2.5 rounded-xl border border-slate-300 text-xs font-bold text-slate-700 hover:bg-slate-50 transition"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={submittingPassword}
                  className="px-6 py-2.5 rounded-xl bg-amber-600 hover:bg-amber-500 text-white text-xs font-black shadow-md shadow-amber-600/20 transition disabled:opacity-50"
                >
                  {submittingPassword ? 'Updating...' : 'Update Password'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

function CandidateDrawer({ candidateSummary, candidateDetail, loading, onClose, onPreviewEmail, onOpenSchedule, onOpenDecision, onOpenInterview, showToast }) {
  const c = candidateDetail || candidateSummary;
  return (
    <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-sm flex justify-end">
      <div className="w-full max-w-2xl bg-white h-full shadow-2xl flex flex-col overflow-hidden animate-slide-left">
        {/* Header */}
        <div className="p-6 bg-slate-900 text-white flex items-center justify-between">
          <div>
            <span className="text-xs font-bold text-blue-400 uppercase tracking-wider">Candidate Profile</span>
            <h2 className="text-xl font-black mt-1">{c.fullName || 'Applicant Profile'}</h2>
            <div className="text-xs text-slate-400 mt-0.5">{c.email || c.senderEmail || c.resumeEmail}</div>
          </div>
          <button onClick={onClose} className="p-2 text-slate-400 hover:text-white text-xl font-bold">&times;</button>
        </div>

        {/* Scrollable Body */}
        <div className="p-6 flex-1 overflow-y-auto space-y-6">
          {loading ? (
            <div className="py-20 text-center text-slate-400">Loading candidate details...</div>
          ) : (
            <>
              {/* Status Banner */}
              <div className="flex items-center justify-between p-4 rounded-2xl bg-slate-50 border border-slate-200">
                <div>
                  <div className="text-xs text-slate-500 font-semibold uppercase">Current Pipeline Status</div>
                  <div className="mt-1">{renderStatusBadge(c.status)}</div>
                </div>
                <div>
                  <div className="text-xs text-slate-500 font-semibold uppercase text-right">ATS Match Score</div>
                  <div className="mt-1 text-right">{renderScoreBadge(c.finalAtsScore ?? c.atsScore ?? c.atsResult?.finalScore)}</div>
                </div>
              </div>

              {/* Candidate Demographic & Profile Quick Info */}
              <div className="grid grid-cols-2 gap-3 p-4 rounded-2xl bg-slate-50 border border-slate-200">
                <div>
                  <div className="text-[11px] font-bold text-slate-500 uppercase tracking-wider">Gender / Demographic</div>
                  <div className="mt-1">{renderGenderBadge(c.gender)}</div>
                </div>
                <div>
                  <div className="text-[11px] font-bold text-slate-500 uppercase tracking-wider">Total Experience</div>
                  <div className="text-sm font-semibold text-slate-800 mt-1">
                    {c.totalExperienceYears ? `${c.totalExperienceYears} years` : 'Not specified'}
                  </div>
                </div>
                <div>
                  <div className="text-[11px] font-bold text-slate-500 uppercase tracking-wider">Education / Degree</div>
                  <div className="text-sm font-semibold text-slate-800 mt-1">
                    {c.degree || c.education || 'Not specified'}
                  </div>
                </div>
                <div>
                  <div className="text-[11px] font-bold text-slate-500 uppercase tracking-wider">Location</div>
                  <div className="text-sm font-semibold text-slate-800 mt-1">
                    {c.location || 'Remote / Unspecified'}
                  </div>
                </div>
              </div>

              {/* Multi-Round Stepper */}
              <InterviewStepper candidate={c} />

              {/* Active Scheduled Interview Card */}
              {(() => {
                const activeInterview = getActiveScheduledInterview(c);
                if (!activeInterview) return null;
                return (
                  <div className="p-4 rounded-2xl bg-gradient-to-r from-blue-50 via-indigo-50 to-purple-50 border border-blue-200 shadow-sm space-y-3">
                    <div className="flex items-center justify-between flex-wrap gap-2">
                      <div className="flex items-center gap-2.5">
                        <div className="w-9 h-9 rounded-xl bg-blue-600 text-white flex items-center justify-center text-lg shadow-md shadow-blue-500/30">
                          🎥
                        </div>
                        <div>
                          <span className="text-[10px] font-extrabold uppercase tracking-wider text-blue-600 bg-blue-100 px-2 py-0.5 rounded-full">
                            Active Scheduled Interview
                          </span>
                          <h4 className="font-black text-sm text-slate-900 mt-0.5">
                            {activeInterview.roundDisplayName || formatRoundName(activeInterview.round)}
                          </h4>
                        </div>
                      </div>
                      <div className="text-right text-xs">
                        <span className="text-slate-500">Scheduled: </span>
                        <span className="font-bold text-slate-800">
                          {activeInterview.interviewDate ? new Date(activeInterview.interviewDate).toLocaleString() : 'Pending'}
                          {activeInterview.durationMinutes ? ` (${activeInterview.durationMinutes} mins)` : ''}
                        </span>
                      </div>
                    </div>

                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 text-xs pt-1">
                      <div className="p-2.5 rounded-xl bg-white/80 border border-blue-100">
                        <div className="text-[10px] font-bold text-slate-500 uppercase">Interviewer Assigned</div>
                        <div className="font-semibold text-slate-800 mt-0.5">{activeInterview.interviewerName || 'Technical Evaluator'}</div>
                        <div className="text-[11px] text-slate-500">{activeInterview.interviewerEmail || 'interviewer@sanvii.com'}</div>
                      </div>
                      <div className="p-2.5 rounded-xl bg-white/80 border border-blue-100 flex flex-col justify-between">
                        <div className="text-[10px] font-bold text-slate-500 uppercase">Google Meet Session</div>
                        <div className="mt-1 flex items-center gap-2">
                          {activeInterview.meetingLink ? (
                            <>
                              <a
                                href={activeInterview.meetingLink}
                                target="_blank"
                                rel="noopener noreferrer"
                                className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-bold shadow-sm transition"
                              >
                                <span>🎥</span>
                                <span>Join Google Meet</span>
                              </a>
                              <button
                                onClick={() => {
                                  navigator.clipboard?.writeText(activeInterview.meetingLink);
                                  if (showToast) showToast('Google Meet link copied to clipboard!', 'info');
                                }}
                                title="Copy Meet link"
                                className="p-1.5 rounded-lg border border-slate-200 bg-white hover:bg-slate-50 text-slate-600 text-xs font-semibold"
                              >
                                📋 Copy
                              </button>
                              <button
                                onClick={async () => {
                                  try {
                                    const res = await fetch(`${API_BASE}/api/v1/candidates/${c.id}/interviews/${activeInterview.id}/resend-invitations`, {
                                      method: 'POST',
                                      headers: getAuthHeaders(auth)
                                    });
                                    const json = await res.json();
                                    if (res.ok && json.success) {
                                      if (showToast) showToast('Interview email invitations dispatched successfully to candidate and interviewer!', 'success');
                                    } else {
                                      if (showToast) showToast(json.message || 'Failed to dispatch email invitations', 'error');
                                    }
                                  } catch(err) {
                                    if (showToast) showToast('Error resending invitations: ' + err.message, 'error');
                                  }
                                }}
                                title="Resend email notifications to Candidate and Interviewer"
                                className="p-1.5 px-2.5 rounded-lg border border-blue-300 bg-blue-50 hover:bg-blue-100 text-blue-700 text-xs font-semibold flex items-center gap-1 shadow-sm transition"
                              >
                                <span>📧</span>
                                <span>Resend Emails</span>
                              </button>
                            </>
                          ) : (
                            <span className="text-slate-400 italic">No link assigned</span>
                          )}
                        </div>
                      </div>
                    </div>

                    <div className="pt-2 border-t border-blue-100 flex items-center justify-between">
                      <span className="text-xs text-slate-500">Interview conducted? Record the evaluator's decision:</span>
                      <button
                        onClick={() => onOpenDecision ? onOpenDecision(c, activeInterview) : onOpenInterview()}
                        className="px-4 py-2 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-bold shadow-md shadow-indigo-600/30 flex items-center gap-1.5"
                      >
                        <span>📝</span>
                        <span>Record Round Decision &rarr;</span>
                      </button>
                    </div>
                  </div>
                );
              })()}

              {/* Ready to Schedule Next Round Card */}
              {canScheduleInterview(c) && !getActiveScheduledInterview(c) && (
                <div className="p-4 rounded-2xl bg-emerald-50 border border-emerald-200 flex items-center justify-between flex-wrap gap-3">
                  <div>
                    <div className="font-bold text-sm text-emerald-950 flex items-center gap-2">
                      <span>📅</span>
                      <span>Next Round Ready: {formatRoundName(getNextPendingRound(c))}</span>
                    </div>
                    <div className="text-xs text-emerald-700 mt-0.5">
                      Candidate is eligible for the next interview round. Set up Google Meet and dual email invites.
                    </div>
                  </div>
                  <button
                    onClick={() => onOpenSchedule ? onOpenSchedule(c) : onOpenInterview()}
                    className="px-4 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-bold shadow-md shadow-emerald-600/30 flex items-center gap-2"
                  >
                    <span>⚡ Schedule Interview &rarr;</span>
                  </button>
                </div>
              )}

              {/* Final Status Celebration / Regret */}
              {c.status === 'SELECTED' && (
                <div className="p-4 rounded-2xl bg-purple-50 border border-purple-200 flex items-center gap-3">
                  <div className="w-10 h-10 rounded-xl bg-purple-600 text-white flex items-center justify-center text-xl shadow-md">
                    🎉
                  </div>
                  <div>
                    <div className="font-black text-sm text-purple-950">Candidate Selected & Hired!</div>
                    <div className="text-xs text-purple-700">All 4 rounds completed successfully. Official selection & offer letter has been dispatched.</div>
                  </div>
                </div>
              )}

              {c.status === 'NOT_SELECTED' && (
                <div className="p-4 rounded-2xl bg-rose-50 border border-rose-200 flex items-center gap-3">
                  <div className="w-10 h-10 rounded-xl bg-rose-600 text-white flex items-center justify-center text-xl shadow-md">
                    🚫
                  </div>
                  <div>
                    <div className="font-black text-sm text-rose-950">Candidate Not Selected</div>
                    <div className="text-xs text-rose-700">Pipeline concluded. Graceful regret notification email dispatched.</div>
                  </div>
                </div>
              )}

              {/* Multi-Round Interview History Timeline */}
              <div className="space-y-3">
                <div className="flex items-center justify-between">
                  <h4 className="font-bold text-xs uppercase text-slate-500 tracking-wider">
                    Interview History & Notes ({c.interviews?.length || 0} Rounds)
                  </h4>
                  {canScheduleInterview(c) && !getActiveScheduledInterview(c) && (
                    <button
                      onClick={() => onOpenSchedule ? onOpenSchedule(c) : onOpenInterview()}
                      className="text-xs font-bold text-blue-600 hover:text-blue-700 flex items-center gap-1"
                    >
                      <span>+ Schedule Round</span>
                    </button>
                  )}
                </div>

                <div className="space-y-2.5">
                  {(c.interviews || []).map((inv, idx) => {
                    const isPending = inv.result === 'PENDING';
                    const isSelected = inv.result === 'SELECTED';
                    const isNotSelected = inv.result === 'NOT_SELECTED';

                    return (
                      <div
                        key={inv.id || idx}
                        className={`p-4 rounded-2xl border transition ${
                          isPending
                            ? 'bg-blue-50/50 border-blue-200'
                            : isSelected
                            ? 'bg-emerald-50/30 border-emerald-200'
                            : 'bg-slate-50 border-slate-200'
                        }`}
                      >
                        <div className="flex items-center justify-between flex-wrap gap-2">
                          <div className="flex items-center gap-2">
                            <span className="font-bold text-sm text-slate-900">
                              {inv.roundDisplayName || formatRoundName(inv.round)}
                            </span>
                            {isPending && (
                              <span className="text-[10px] font-extrabold uppercase px-2 py-0.5 rounded-full bg-blue-100 text-blue-700 border border-blue-200 animate-pulse">
                                🎥 Scheduled
                              </span>
                            )}
                            {isSelected && (
                              <span className="text-[10px] font-extrabold uppercase px-2 py-0.5 rounded-full bg-emerald-100 text-emerald-800 border border-emerald-200">
                                ✓ Passed Round
                              </span>
                            )}
                            {isNotSelected && (
                              <span className="text-[10px] font-extrabold uppercase px-2 py-0.5 rounded-full bg-rose-100 text-rose-700 border border-rose-200">
                                ✕ Not Selected
                              </span>
                            )}
                          </div>
                          <div className="text-xs text-slate-500">
                            {inv.interviewDate ? new Date(inv.interviewDate).toLocaleString() : 'Date pending'}
                            {inv.durationMinutes ? ` (${inv.durationMinutes}m)` : ''}
                          </div>
                        </div>

                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 mt-2 text-xs">
                          <div>
                            <span className="text-slate-500">Interviewer: </span>
                            <span className="font-semibold text-slate-800">{inv.interviewerName || 'Lead Evaluator'}</span>
                            {inv.interviewerEmail && (
                              <span className="text-slate-400 text-[11px] block">{inv.interviewerEmail}</span>
                            )}
                          </div>
                          {inv.meetingLink && (
                            <div>
                              <span className="text-slate-500">Google Meet: </span>
                              <a
                                href={inv.meetingLink}
                                target="_blank"
                                rel="noopener noreferrer"
                                className="font-mono text-blue-600 hover:underline inline-flex items-center gap-1 text-[11px]"
                              >
                                <span>🎥</span>
                                <span>Join Meeting &rarr;</span>
                              </a>
                            </div>
                          )}
                        </div>

                        {inv.notes && (
                          <div className="mt-2 text-xs bg-white/70 p-2.5 rounded-xl border border-slate-100 text-slate-600">
                            <strong className="text-slate-700">Agenda / Instructions: </strong>
                            {inv.notes}
                          </div>
                        )}

                        {/* Rich Interviewer Evaluation Scorecard */}
                        {(inv.technicalRating || inv.overallRating || inv.feedback) && (
                          <div className="mt-3 bg-white p-3.5 rounded-xl border border-slate-200 shadow-sm space-y-2.5">
                            <div className="flex items-center justify-between">
                              <div className="flex items-center gap-1.5">
                                <span className="text-amber-500 font-black text-sm">⭐</span>
                                <span className="font-extrabold text-xs text-slate-900">
                                  Interviewer Scorecard: {inv.overallRating ? `${inv.overallRating} / 5.0` : 'Feedback Recorded'}
                                </span>
                              </div>
                              {inv.result === 'SELECTED' ? (
                                <span className="text-[10px] font-bold text-emerald-800 bg-emerald-50 border border-emerald-200 px-2.5 py-0.5 rounded-full">
                                  ✓ Good (Recommended to Advance)
                                </span>
                              ) : inv.result === 'NOT_SELECTED' ? (
                                <span className="text-[10px] font-bold text-rose-800 bg-rose-50 border border-rose-200 px-2.5 py-0.5 rounded-full">
                                  ✕ Bad (Not Recommended)
                                </span>
                              ) : null}
                            </div>

                            {inv.technicalRating && (
                              <div className="grid grid-cols-2 gap-2 text-xs bg-slate-50 p-2.5 rounded-xl border border-slate-100">
                                <div className="flex items-center justify-between">
                                  <span className="text-slate-600 font-medium">Technical:</span>
                                  <span className="font-bold text-slate-800 flex items-center gap-1">
                                    <StarRatingDisplay value={inv.technicalRating} /> {inv.technicalRating}/5
                                  </span>
                                </div>
                                <div className="flex items-center justify-between">
                                  <span className="text-slate-600 font-medium">Communication:</span>
                                  <span className="font-bold text-slate-800 flex items-center gap-1">
                                    <StarRatingDisplay value={inv.communicationRating} /> {inv.communicationRating}/5
                                  </span>
                                </div>
                                <div className="flex items-center justify-between">
                                  <span className="text-slate-600 font-medium">Confidence:</span>
                                  <span className="font-bold text-slate-800 flex items-center gap-1">
                                    <StarRatingDisplay value={inv.confidenceRating} /> {inv.confidenceRating}/5
                                  </span>
                                </div>
                                <div className="flex items-center justify-between">
                                  <span className="text-slate-600 font-medium">Body Language:</span>
                                  <span className="font-bold text-slate-800 flex items-center gap-1">
                                    <StarRatingDisplay value={inv.bodyLanguageRating} /> {inv.bodyLanguageRating}/5
                                  </span>
                                </div>
                              </div>
                            )}

                            {(inv.comments || inv.feedback) && (
                              <div className="text-xs text-slate-700 bg-amber-50/40 p-2.5 rounded-xl border border-amber-200/50">
                                <span className="font-bold text-slate-900 block mb-1">💬 Interviewer Feedback & Comments:</span>
                                <p className="italic text-slate-700 font-sans leading-relaxed">"{inv.comments || inv.feedback}"</p>
                              </div>
                            )}
                          </div>
                        )}

                        {isPending && (
                          <div className="mt-3 pt-2.5 border-t border-blue-100 flex items-center justify-between flex-wrap gap-2">
                            <span className="text-xs text-blue-700 font-semibold">Evaluation awaiting submission</span>
                            <div className="flex items-center gap-2">
                              <button
                                onClick={() => {
                                  const token = inv.accessToken || inv.id;
                                  window.open(`/?portal=interviewer&token=${token}`, '_blank');
                                }}
                                className="px-2.5 py-1.5 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-bold shadow-sm flex items-center gap-1"
                                title="Open direct feedback submission portal link sent to interviewer"
                              >
                                <span>⭐</span>
                                <span>Interviewer Portal &rarr;</span>
                              </button>
                              <button
                                onClick={() => onOpenDecision ? onOpenDecision(c, inv) : onOpenInterview()}
                                className="px-3 py-1.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white text-xs font-bold shadow-sm"
                              >
                                📝 Record HR Decision
                              </button>
                            </div>
                          </div>
                        )}
                      </div>
                    );
                  })}

                  {(!c.interviews || c.interviews.length === 0) && (
                    <div className="text-xs text-slate-400 italic py-4 text-center bg-slate-50 rounded-2xl border border-slate-200">
                      No interview rounds scheduled yet. Click "Schedule Round" to initiate the pipeline.
                    </div>
                  )}
                </div>
              </div>

              {/* ATS Scoring Breakdown */}
              {c.atsResult && (
                <div className="space-y-3">
                  <h4 className="font-bold text-sm text-slate-900 uppercase tracking-wider text-xs">ATS Evaluation Breakdown</h4>
                  <div className="p-4 rounded-2xl bg-slate-50 border border-slate-200 space-y-3">
                    <div className="text-xs text-slate-700 leading-relaxed">
                      <strong>AI Summary:</strong> {c.atsResult.aiSummary || 'No AI summary available'}
                    </div>

                    {c.atsResult.matchedSkills?.length > 0 && (
                      <div>
                        <div className="text-[11px] font-bold text-emerald-800 uppercase mb-1">Matched Skills:</div>
                        <div className="flex flex-wrap gap-1.5">
                          {c.atsResult.matchedSkills.map((sk, i) => (
                            <span key={i} className="text-xs bg-emerald-100 text-emerald-800 px-2 py-0.5 rounded font-medium">✓ {sk}</span>
                          ))}
                        </div>
                      </div>
                    )}

                    {c.atsResult.missingSkills?.length > 0 && (
                      <div>
                        <div className="text-[11px] font-bold text-rose-800 uppercase mb-1">Missing Job Requirements:</div>
                        <div className="flex flex-wrap gap-1.5">
                          {c.atsResult.missingSkills.map((sk, i) => (
                            <span key={i} className="text-xs bg-rose-100 text-rose-800 px-2 py-0.5 rounded font-medium">&times; {sk}</span>
                          ))}
                        </div>
                      </div>
                    )}
                  </div>
                </div>
              )}

              {/* Dispatched Emails History */}
              <div className="space-y-3">
                <h4 className="font-bold text-xs uppercase text-slate-500 tracking-wider">Dispatched Emails History</h4>
                {(c.emailCommunications || []).map(em => (
                  <div key={em.id} className="p-4 rounded-2xl border border-slate-200 bg-white flex items-center justify-between">
                    <div>
                      <div className="font-semibold text-xs text-slate-900">{em.subject}</div>
                      <div className="text-[11px] text-slate-500 mt-0.5">Type: {em.emailType}</div>
                    </div>
                    <button
                      onClick={() => onPreviewEmail(em)}
                      className="px-2.5 py-1 text-xs font-semibold text-blue-600 hover:bg-blue-50 rounded"
                    >
                      View HTML
                    </button>
                  </div>
                ))}
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
}

function EmailModal({ email, onClose }) {
  return (
    <div className="fixed inset-0 z-50 bg-slate-900/70 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="w-full max-w-3xl bg-white rounded-3xl shadow-2xl flex flex-col max-h-[90vh] overflow-hidden">
        <div className="p-5 bg-slate-900 text-white flex items-center justify-between">
          <h3 className="font-bold text-base">{email.subject}</h3>
          <button onClick={onClose} className="p-2 text-slate-400 hover:text-white text-xl font-bold">&times;</button>
        </div>
        <div className="p-6 flex-1 overflow-y-auto bg-slate-100">
          <div className="bg-white p-4 rounded-xl shadow-inner" dangerouslySetInnerHTML={{ __html: email.bodyContent }} />
        </div>
      </div>
    </div>
  );
}

// ==========================================
// MULTI-ROUND INTERVIEW COMPONENTS
// ==========================================
export function InterviewStepper({ candidate }) {
  if (!candidate) return null;
  const status = candidate.status;
  const interviews = candidate.interviews || [];

  const steps = [
    { key: 'SCREENED', label: 'Resume Screened', desc: 'ATS ≥ 80%' },
    { key: 'L1_TECHNICAL', label: 'L1 Technical', desc: 'Tech Screening' },
    { key: 'L2_TECHNICAL', label: 'L2 Technical', desc: 'Coding & Arch' },
    { key: 'MANAGERIAL', label: 'Managerial', desc: 'Fit & Leadership' },
    { key: 'HR_ROUND', label: 'HR Round', desc: 'Culture & Offer' },
    { key: 'HIRED', label: 'Offer / Hired', desc: 'Selection Sent' }
  ];

  const getStepStatus = (index) => {
    // Step 0: Resume Screened
    if (index === 0) {
      if (status === 'REJECTED') return { state: 'failed', badge: 'Screening Failed' };
      if (status === 'REVIEW_REQUIRED') return { state: 'current', badge: 'Review Required' };
      return { state: 'completed', badge: 'Passed' };
    }

    // Step 5: Final Offer / Hired
    if (index === 5) {
      if (status === 'SELECTED') return { state: 'completed', badge: '🎉 Hired' };
      return { state: 'upcoming', badge: 'Pending' };
    }

    // Intermediate Interview Steps (1: L1, 2: L2, 3: Managerial, 4: HR)
    const roundKeys = ['L1_TECHNICAL', 'L2_TECHNICAL', 'MANAGERIAL', 'HR_ROUND'];
    const rKey = roundKeys[index - 1];
    const interview = interviews.find(i => i.round === rKey);

    if (interview) {
      if (interview.result === 'SELECTED') return { state: 'completed', badge: 'Passed' };
      if (interview.result === 'NOT_SELECTED') return { state: 'failed', badge: 'Not Selected' };
      if (interview.result === 'PENDING') return { state: 'scheduled', badge: 'Scheduled' };
    }

    // Status-based detection fallback
    const schedStatuses = {
      L1_TECHNICAL: 'L1_SCHEDULED',
      L2_TECHNICAL: 'L2_SCHEDULED',
      MANAGERIAL: 'MANAGERIAL_SCHEDULED',
      HR_ROUND: 'HR_SCHEDULED'
    };
    if (status === schedStatuses[rKey]) return { state: 'scheduled', badge: 'Scheduled' };

    const passedStatuses = {
      L1_TECHNICAL: ['L1_PASSED', 'L2_SCHEDULED', 'L2_PASSED', 'MANAGERIAL_SCHEDULED', 'MANAGERIAL_PASSED', 'HR_SCHEDULED', 'SELECTED'],
      L2_TECHNICAL: ['L2_PASSED', 'MANAGERIAL_SCHEDULED', 'MANAGERIAL_PASSED', 'HR_SCHEDULED', 'SELECTED'],
      MANAGERIAL: ['MANAGERIAL_PASSED', 'HR_SCHEDULED', 'SELECTED'],
      HR_ROUND: ['SELECTED']
    };
    if (passedStatuses[rKey]?.includes(status)) return { state: 'completed', badge: 'Passed' };

    if (status === 'NOT_SELECTED') {
      const last = interviews.slice().reverse().find(i => i.round === rKey);
      if (last && last.result === 'NOT_SELECTED') return { state: 'failed', badge: 'Not Selected' };
    }

    const readyStatuses = {
      L1_TECHNICAL: ['SHORTLISTED'],
      L2_TECHNICAL: ['L1_PASSED'],
      MANAGERIAL: ['L2_PASSED'],
      HR_ROUND: ['MANAGERIAL_PASSED']
    };
    if (readyStatuses[rKey]?.includes(status)) return { state: 'ready', badge: 'Ready' };

    return { state: 'upcoming', badge: 'Upcoming' };
  };

  return (
    <div className="p-5 rounded-2xl bg-white border border-slate-200/90 shadow-sm space-y-3">
      <div className="flex items-center justify-between">
        <span className="text-[11px] font-extrabold uppercase tracking-wider text-slate-500">
          Pipeline Progression: L1 &rarr; L2 &rarr; Managerial &rarr; HR
        </span>
        <span className="text-xs font-bold text-slate-700">
          {renderStatusBadge(status)}
        </span>
      </div>

      <div className="relative pt-2 pb-1">
        <div className="grid grid-cols-6 gap-2">
          {steps.map((st, i) => {
            const { state, badge } = getStepStatus(i);
            
            let circleClass = 'bg-slate-100 text-slate-400 border border-slate-300';
            let icon = i + 1;
            let badgeClass = 'bg-slate-100 text-slate-600 border-slate-200';
            let titleClass = 'text-slate-600';

            if (state === 'completed') {
              circleClass = 'bg-emerald-600 text-white shadow-md shadow-emerald-600/30 ring-2 ring-emerald-200';
              icon = '✓';
              badgeClass = 'bg-emerald-50 text-emerald-700 border-emerald-200 font-bold';
              titleClass = 'text-emerald-900 font-bold';
            } else if (state === 'scheduled') {
              circleClass = 'bg-blue-600 text-white shadow-md shadow-blue-600/30 ring-4 ring-blue-100 animate-pulse';
              icon = '🎥';
              badgeClass = 'bg-blue-50 text-blue-700 border-blue-200 font-bold animate-pulse';
              titleClass = 'text-blue-900 font-bold';
            } else if (state === 'ready') {
              circleClass = 'bg-amber-50 text-amber-700 border-2 border-dashed border-amber-500 ring-2 ring-amber-100';
              icon = '+';
              badgeClass = 'bg-amber-50 text-amber-700 border-amber-200 font-bold';
              titleClass = 'text-amber-900 font-semibold';
            } else if (state === 'failed') {
              circleClass = 'bg-rose-600 text-white shadow-md shadow-rose-600/30 ring-2 ring-rose-200';
              icon = '✕';
              badgeClass = 'bg-rose-50 text-rose-700 border-rose-200 font-bold';
              titleClass = 'text-rose-900 font-bold';
            }

            return (
              <div key={st.key} className="flex flex-col items-center text-center group">
                <div className={`w-9 h-9 rounded-full flex items-center justify-center text-xs font-black transition mb-2 ${circleClass}`}>
                  {icon}
                </div>
                <div className={`text-[11px] leading-tight line-clamp-1 ${titleClass}`}>
                  {st.label}
                </div>
                <div className="text-[10px] text-slate-400 mt-0.5 line-clamp-1 hidden sm:block">
                  {st.desc}
                </div>
                <span className={`inline-block mt-1 text-[9px] px-1.5 py-0.5 rounded-full border ${badgeClass}`}>
                  {badge}
                </span>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}

export function ScheduleInterviewModal({ candidate, form, setForm, registeredInterviewers, submitting, onClose, onSubmit }) {
  const roundOptions = [
    { value: 'L1_TECHNICAL', label: 'L1: Technical Screening', desc: 'Core fundamentals & live problem solving' },
    { value: 'L2_TECHNICAL', label: 'L2: Advanced Technical & Coding', desc: 'System design, live architecture & coding' },
    { value: 'MANAGERIAL', label: 'Managerial Round', desc: 'Team fit, ownership & technical leadership' },
    { value: 'HR_ROUND', label: 'HR Round', desc: 'Cultural fit, compensation & final evaluation' }
  ];

  const handleGenerateSanviiLiveRoom = () => {
    const cName = (candidate.fullName || 'Candidate').replace(/[^a-zA-Z0-9]/g, '') || 'Candidate';
    const rName = form.round || 'L1_TECHNICAL';
    const rnd = Math.random().toString(36).substring(2, 8);
    setForm({ ...form, meetingLink: `https://meet.jit.si/Sanvii-Interview-${cName}-${rName}-${rnd}` });
  };

  const handleGenerateGoogleMeetCode = () => {
    const chars = 'abcdefghijklmnopqrstuvwxyz';
    const rnd = (len) => Array.from({length: len}, () => chars[Math.floor(Math.random() * chars.length)]).join('');
    setForm({ ...form, meetingLink: `https://meet.google.com/${rnd(3)}-${rnd(4)}-${rnd(3)}` });
  };

  return (
    <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="w-full max-w-xl bg-white rounded-3xl shadow-2xl overflow-hidden">
        {/* Header */}
        <div className="p-6 bg-gradient-to-r from-blue-900 via-indigo-900 to-slate-900 text-white flex items-center justify-between">
          <div>
            <span className="text-xs uppercase tracking-wider font-bold text-blue-400">Multi-Round Pipeline</span>
            <h3 className="font-black text-lg mt-0.5">Schedule Interview: {candidate.fullName}</h3>
            <div className="text-xs text-slate-300">Candidate Email: {candidate.email || candidate.senderEmail}</div>
          </div>
          <button onClick={onClose} className="p-2 text-slate-400 hover:text-white text-xl font-bold">&times;</button>
        </div>

        {/* Form Body */}
        <div className="p-6 space-y-4 max-h-[75vh] overflow-y-auto custom-scrollbar">
          {/* Round Selector */}
          <div>
            <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1.5">
              Select Interview Round *
            </label>
            <div className="grid grid-cols-2 gap-2.5">
              {roundOptions.map(r => (
                <div
                  key={r.value}
                  onClick={() => setForm({ ...form, round: r.value })}
                  className={`p-3 rounded-xl border cursor-pointer transition ${
                    form.round === r.value
                      ? 'border-blue-500 bg-blue-50/80 ring-2 ring-blue-500/30'
                      : 'border-slate-200 hover:border-slate-300 bg-white'
                  }`}
                >
                  <div className="font-bold text-xs text-slate-900 flex items-center justify-between">
                    <span>{r.label}</span>
                    {form.round === r.value && <span className="text-blue-600 font-extrabold text-sm">✓</span>}
                  </div>
                  <div className="text-[11px] text-slate-500 mt-0.5">{r.desc}</div>
                </div>
              ))}
            </div>
          </div>

          {/* Optional Quick Select Registered Interviewer */}
          {registeredInterviewers && registeredInterviewers.length > 0 && (
            <div>
              <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1">
                Select Registered Interviewer (Auto-fills Name & Email)
              </label>
              <select
                className="w-full text-xs font-medium px-3.5 py-2.5 bg-slate-50 border border-slate-300 rounded-xl focus:ring-2 focus:ring-blue-500 focus:outline-none"
                onChange={e => {
                  const selected = registeredInterviewers.find(i => i.id === e.target.value);
                  if (selected) {
                    setForm({
                      ...form,
                      interviewerName: selected.fullName,
                      interviewerEmail: selected.email
                    });
                  }
                }}
                defaultValue=""
              >
                <option value="" disabled>-- Choose registered interviewer panelist --</option>
                {registeredInterviewers.filter(i => i.active).map(i => (
                  <option key={i.id} value={i.id}>
                    {i.fullName} ({i.designation} &bull; {i.email})
                  </option>
                ))}
              </select>
            </div>
          )}

          {/* Interviewer details */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1">
                Interviewer Name *
              </label>
              <input
                type="text"
                value={form.interviewerName}
                onChange={e => setForm({ ...form, interviewerName: e.target.value })}
                placeholder="e.g. Lead Technical Architect"
                className="w-full text-sm px-3.5 py-2.5 border border-slate-200 rounded-xl focus:ring-2 focus:ring-blue-500 focus:outline-none"
              />
            </div>
            <div>
              <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1">
                Interviewer Email *
              </label>
              <input
                type="email"
                value={form.interviewerEmail}
                onChange={e => setForm({ ...form, interviewerEmail: e.target.value })}
                placeholder="interviewer@sanvii.com"
                className="w-full text-sm px-3.5 py-2.5 border border-slate-200 rounded-xl focus:ring-2 focus:ring-blue-500 focus:outline-none"
              />
            </div>
          </div>

          {/* Schedule Date & Duration */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <div>
              <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1">
                Date & Time *
              </label>
              <input
                type="datetime-local"
                value={form.interviewDate}
                onChange={e => setForm({ ...form, interviewDate: e.target.value })}
                className="w-full text-sm px-3.5 py-2.5 border border-slate-200 rounded-xl focus:ring-2 focus:ring-blue-500 focus:outline-none"
              />
            </div>
            <div>
              <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1">
                Duration (Minutes)
              </label>
              <select
                value={form.durationMinutes}
                onChange={e => setForm({ ...form, durationMinutes: parseInt(e.target.value) })}
                className="w-full text-sm px-3.5 py-2.5 border border-slate-200 rounded-xl focus:ring-2 focus:ring-blue-500 focus:outline-none bg-white"
              >
                <option value={30}>30 Minutes</option>
                <option value={45}>45 Minutes (Standard)</option>
                <option value={60}>60 Minutes</option>
                <option value={90}>90 Minutes</option>
              </select>
            </div>
          </div>

          {/* Video Meeting Link with Instant Real-Time Generators */}
          <div>
            <div className="flex items-center justify-between mb-1.5">
              <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider">
                Interview Video Meeting Room Link *
              </label>
              <span className="text-[11px] text-emerald-600 font-semibold bg-emerald-50 px-2 py-0.5 rounded-full border border-emerald-200">
                ● Functioning Link
              </span>
            </div>
            <div className="relative">
              <input
                type="url"
                value={form.meetingLink}
                onChange={e => setForm({ ...form, meetingLink: e.target.value })}
                placeholder="https://meet.jit.si/... or https://meet.google.com/xxx-yyyy-zzz"
                className="w-full text-sm pl-9 pr-3.5 py-2.5 border border-slate-200 rounded-xl focus:ring-2 focus:ring-blue-500 focus:outline-none font-mono"
              />
              <span className="absolute left-3 top-3 text-slate-400">🎥</span>
            </div>

            {/* Instant Link Helper Buttons */}
            <div className="mt-2 flex flex-wrap items-center gap-2">
              <button
                type="button"
                onClick={handleGenerateSanviiLiveRoom}
                className="px-2.5 py-1 rounded-lg bg-emerald-50 hover:bg-emerald-100 text-emerald-700 text-xs font-bold border border-emerald-200 flex items-center gap-1 transition shadow-xs"
                title="Generate guaranteed functioning room (instant zero-login HD video & screen share)"
              >
                <span>⚡ Sanvii Live Room (Active)</span>
              </button>
              <a
                href="https://meet.google.com/new"
                target="_blank"
                rel="noopener noreferrer"
                className="px-2.5 py-1 rounded-lg bg-blue-50 hover:bg-blue-100 text-blue-700 text-xs font-bold border border-blue-200 flex items-center gap-1 transition shadow-xs"
                title="Launch meet.google.com/new to create an official Google Meet room and paste it here"
              >
                <span>🌐 Launch Google Meet (meet.google.com/new) &nearr;</span>
              </a>
              <button
                type="button"
                onClick={handleGenerateGoogleMeetCode}
                className="px-2.5 py-1 rounded-lg bg-slate-100 hover:bg-slate-200 text-slate-700 text-xs font-bold border border-slate-200 flex items-center gap-1 transition"
                title="Generate strictly syntax-valid Google Meet code (xxx-yyyy-zzz)"
              >
                <span>🎲 Google Meet Code</span>
              </button>
            </div>
            <div className="text-[11px] text-slate-400 mt-1.5">
              Both candidate and interviewer will receive this functioning link via email. Click &quot;Launch Google Meet&quot; if you want to paste a real Google Meet room instead.
            </div>
          </div>

          {/* Instructions / Notes */}
          <div>
            <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1">
              Evaluation Agenda & Instructions
            </label>
            <textarea
              rows="2"
              value={form.notes}
              onChange={e => setForm({ ...form, notes: e.target.value })}
              placeholder="e.g. Focus on Spring Boot microservices, multithreading, and RESTful principles."
              className="w-full text-sm px-3.5 py-2.5 border border-slate-200 rounded-xl focus:ring-2 focus:ring-blue-500 focus:outline-none"
            ></textarea>
          </div>

          {/* Action Buttons */}
          <div className="pt-2 flex items-center justify-end gap-3 border-t border-slate-100">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2.5 rounded-xl border border-slate-200 text-slate-700 text-sm font-semibold hover:bg-slate-50"
            >
              Cancel
            </button>
            <button
              type="button"
              onClick={onSubmit}
              disabled={submitting || !form.interviewerName || !form.interviewDate}
              className="px-6 py-2.5 rounded-xl bg-blue-600 hover:bg-blue-500 text-white text-sm font-bold shadow-lg shadow-blue-600/30 disabled:opacity-60 flex items-center gap-2"
            >
              <span>{submitting ? '⏳' : '🚀'}</span>
              <span>{submitting ? 'Scheduling & Dispatching Emails...' : 'Schedule & Send Dual Invites'}</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

export function StarRating({ value, onChange, label, disabled = false, helperText = '' }) {
  const [hover, setHover] = useState(0);
  const descriptions = ['', 'Poor (1/5)', 'Below Average (2/5)', 'Average (3/5)', 'Good / Strong (4/5)', 'Exceptional (5/5)'];
  return (
    <div className="space-y-1.5">
      <div className="flex items-center justify-between">
        <label className="text-xs font-bold text-slate-700 tracking-wide">{label} *</label>
        <span className="text-xs font-bold text-indigo-700 bg-indigo-50 border border-indigo-100 px-2 py-0.5 rounded-md">
          {descriptions[hover || value] || `${value || 0} / 5`}
        </span>
      </div>
      <div className="flex items-center gap-2">
        {[1, 2, 3, 4, 5].map((star) => (
          <button
            type="button"
            key={star}
            disabled={disabled}
            onMouseEnter={() => !disabled && setHover(star)}
            onMouseLeave={() => !disabled && setHover(0)}
            onClick={() => !disabled && onChange(star)}
            className={`text-2xl transition transform ${!disabled ? 'hover:scale-125 cursor-pointer' : 'cursor-default'} ${
              star <= (hover || value)
                ? 'text-amber-400 drop-shadow-sm'
                : 'text-slate-200 hover:text-amber-200'
            }`}
          >
            ★
          </button>
        ))}
      </div>
      {helperText && <p className="text-[11px] text-slate-400">{helperText}</p>}
    </div>
  );
}

export function StarRatingDisplay({ value, max = 5, size = 'sm' }) {
  const numericVal = Math.round(Number(value) || 0);
  return (
    <span className="inline-flex items-center gap-0.5 text-amber-400 font-serif">
      {[1, 2, 3, 4, 5].map((star) => (
        <span key={star} className={size === 'xs' ? 'text-xs leading-none' : size === 'lg' ? 'text-lg leading-none' : 'text-sm leading-none'}>
          {star <= numericVal ? '★' : '☆'}
        </span>
      ))}
    </span>
  );
}

export function InterviewerPortalView({ interviewer, token, interviewId, onLogout, onSwitchToHR, candidates, showToast, onRefreshData }) {
  const [session, setSession] = useState(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [submittedSuccess, setSubmittedSuccess] = useState(false);
  const [assignedInterviews, setAssignedInterviews] = useState([]);
  const [currentToken, setCurrentToken] = useState(token || '');

  const [feedbackForm, setFeedbackForm] = useState({
    technicalRating: 5,
    communicationRating: 4,
    confidenceRating: 4,
    bodyLanguageRating: 5,
    result: 'SELECTED',
    comments: '',
    interviewerName: interviewer?.fullName || ''
  });

  // Load assigned interviews for logged-in interviewer
  useEffect(() => {
    if (interviewer?.email) {
      fetchAssignedInterviews(interviewer.email);
    }
  }, [interviewer]);

  useEffect(() => {
    if (currentToken) {
      fetchSessionByToken(currentToken);
    } else if (interviewId) {
      fetchSessionById(interviewId);
    } else {
      setLoading(false);
    }
  }, [currentToken, interviewId]);

  const fetchAssignedInterviews = async (email) => {
    try {
      const res = await fetch(`${API_BASE}/api/v1/interviewer/assigned?email=${encodeURIComponent(email)}`);
      const json = await res.json();
      if (res.ok && json.success) {
        setAssignedInterviews(json.data || []);
      }
    } catch (e) {
      console.error("Failed fetching assigned interviews", e);
    }
  };

  const fetchSessionByToken = async (tok) => {
    setLoading(true);
    try {
      const res = await fetch(`${API_BASE}/api/v1/interviewer/session?token=${encodeURIComponent(tok)}`);
      const json = await res.json();
      if (res.ok && json.success) {
        setSession(json.data);
        if (json.data.technicalRating) {
          setFeedbackForm({
            technicalRating: json.data.technicalRating,
            communicationRating: json.data.communicationRating,
            confidenceRating: json.data.confidenceRating,
            bodyLanguageRating: json.data.bodyLanguageRating,
            result: json.data.result || 'SELECTED',
            comments: json.data.comments || '',
            interviewerName: json.data.interviewerName || ''
          });
        } else if (json.data.interviewerName) {
          setFeedbackForm(f => ({ ...f, interviewerName: json.data.interviewerName }));
        }
      } else {
        showToast(json.message || 'Interview session not found with this token.', 'error');
      }
    } catch (e) {
      showToast('Error loading interview session: ' + e.message, 'error');
    } finally {
      setLoading(false);
    }
  };

  const fetchSessionById = async (id) => {
    setLoading(true);
    try {
      const res = await fetch(`${API_BASE}/api/v1/interviewer/interview/${encodeURIComponent(id)}`);
      const json = await res.json();
      if (res.ok && json.success) {
        setSession(json.data);
        if (json.data.technicalRating) {
          setFeedbackForm({
            technicalRating: json.data.technicalRating,
            communicationRating: json.data.communicationRating,
            confidenceRating: json.data.confidenceRating,
            bodyLanguageRating: json.data.bodyLanguageRating,
            result: json.data.result || 'SELECTED',
            comments: json.data.comments || '',
            interviewerName: json.data.interviewerName || ''
          });
        } else if (json.data.interviewerName) {
          setFeedbackForm(f => ({ ...f, interviewerName: json.data.interviewerName }));
        }
      }
    } catch (e) {
      showToast('Error loading interview: ' + e.message, 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleLookupByEmail = async (e) => {
    if (e) e.preventDefault();
    if (!emailLookup.trim()) return;
    setLookupLoading(true);
    try {
      const res = await fetch(`${API_BASE}/api/v1/interviewer/my-interviews?email=${encodeURIComponent(emailLookup.trim())}`);
      const json = await res.json();
      if (res.ok && json.success) {
        setAssignedInterviews(json.data || []);
        if (!json.data || json.data.length === 0) {
          showToast('No assigned interviews found for: ' + emailLookup, 'info');
        }
      } else {
        showToast('Lookup failed', 'error');
      }
    } catch (err) {
      showToast('Lookup error: ' + err.message, 'error');
    } finally {
      setLookupLoading(false);
    }
  };

  const handleSubmitFeedback = async (e) => {
    if (e) e.preventDefault();
    if (!session) return;
    if (!feedbackForm.comments.trim()) {
      showToast('Please provide feedback and notes in the comment box.', 'error');
      return;
    }

    setSubmitting(true);
    try {
      const endpoint = session.accessToken
        ? `${API_BASE}/api/v1/interviewer/session/feedback?token=${encodeURIComponent(session.accessToken)}`
        : `${API_BASE}/api/v1/interviewer/interview/${session.interviewId}/feedback`;

      const payload = {
        technicalRating: feedbackForm.technicalRating,
        communicationRating: feedbackForm.communicationRating,
        confidenceRating: feedbackForm.confidenceRating,
        bodyLanguageRating: feedbackForm.bodyLanguageRating,
        result: feedbackForm.result,
        comments: feedbackForm.comments,
        interviewerName: feedbackForm.interviewerName || session.interviewerName
      };

      const res = await fetch(endpoint, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });
      const json = await res.json();
      if (res.ok && json.success) {
        setSubmittedSuccess(true);
        showToast('Evaluation submitted successfully! Feedback is now live on the HR Dashboard.', 'success');
        if (onRefreshData) onRefreshData();
      } else {
        showToast(json.message || 'Failed to submit feedback', 'error');
      }
    } catch (err) {
      showToast('Submission error: ' + err.message, 'error');
    } finally {
      setSubmitting(false);
    }
  };

  const liveAverage = (
    (feedbackForm.technicalRating +
      feedbackForm.communicationRating +
      feedbackForm.confidenceRating +
      feedbackForm.bodyLanguageRating) / 4
  ).toFixed(2);

  return (
    <div className="min-h-screen bg-slate-900 text-slate-100 flex flex-col font-sans">
      {/* Header */}
      <header className="bg-slate-950/80 backdrop-blur-md border-b border-slate-800 sticky top-0 z-30">
        <div className="max-w-6xl mx-auto px-4 sm:px-6 h-16 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-white flex items-center justify-center p-1 shadow-md">
              <img
                src="/sanvii-icon.png"
                alt="Sanvii Techmet"
                className="w-full h-full object-contain"
                onError={(e) => { if (window.SANVII_ICON_DATA) e.target.src = window.SANVII_ICON_DATA; }}
              />
            </div>
            <div>
              <div className="font-extrabold text-base sm:text-lg flex items-center gap-2 text-white">
                <span>Sanvii ATS Interviewer Portal</span>
                <span className="text-[10px] bg-amber-500/20 text-amber-300 font-bold px-2 py-0.5 rounded-full border border-amber-500/30">
                  Panelist View
                </span>
              </div>
              <p className="text-[11px] text-slate-400">Candidate Evaluation & Feedback Submission Engine</p>
            </div>
          </div>

          <div className="flex items-center gap-3">
            {interviewer && (
              <div className="bg-slate-900 border border-slate-800 rounded-xl px-3 py-1.5 flex items-center gap-2.5 text-xs">
                <div className="w-6 h-6 rounded-lg bg-amber-500/20 text-amber-400 font-bold flex items-center justify-center text-[10px] uppercase">
                  {interviewer.fullName ? interviewer.fullName.substring(0, 2) : 'IN'}
                </div>
                <div className="hidden sm:block text-left">
                  <div className="font-bold text-white text-[11px] leading-none">{interviewer.fullName}</div>
                  <div className="text-[10px] text-amber-400 font-mono mt-0.5 leading-none">{interviewer.email}</div>
                </div>
              </div>
            )}
            {onLogout && (
              <button
                type="button"
                onClick={onLogout}
                className="px-3 py-1.5 rounded-xl bg-red-500/10 text-red-400 hover:bg-red-500/20 border border-red-500/20 text-xs font-bold transition flex items-center gap-1.5"
                title="Sign Out of Interviewer Portal"
              >
                <span>🚪</span>
                <span className="hidden sm:inline">Sign Out</span>
              </button>
            )}
            <button
              type="button"
              onClick={onSwitchToHR}
              className="px-3 py-1.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 border border-slate-700 text-xs font-bold transition flex items-center gap-1.5"
            >
              <span>🏛️</span>
              <span>Switch to HR Admin</span>
            </button>
          </div>
        </div>
      </header>

      {/* Main Body */}
      <main className="max-w-4xl mx-auto px-4 sm:px-6 py-8 flex-1 w-full space-y-6">
        {loading ? (
          <div className="py-24 text-center space-y-3">
            <div className="w-10 h-10 border-4 border-amber-400/30 border-t-amber-400 rounded-full animate-spin mx-auto"></div>
            <div className="text-sm font-semibold text-slate-400">Loading interview session details...</div>
          </div>
        ) : submittedSuccess ? (
          <div className="bg-slate-950 border border-emerald-500/40 rounded-3xl p-8 sm:p-10 shadow-2xl text-center space-y-6 animate-fade-in">
            <div className="w-20 h-20 rounded-full bg-emerald-500/20 border-2 border-emerald-500/50 flex items-center justify-center text-4xl mx-auto text-emerald-400 shadow-lg shadow-emerald-500/20">
              ✓
            </div>
            <div className="space-y-2">
              <h2 className="text-2xl sm:text-3xl font-black text-white">Interview Evaluation Submitted!</h2>
              <p className="text-sm text-slate-400 max-w-lg mx-auto">
                Your assessment for <strong className="text-white">{session?.candidateName}</strong> ({session?.roundDisplayName}) has been successfully saved and is now live on the HR Dashboard.
              </p>
            </div>

            <div className="bg-slate-900/90 border border-slate-800 rounded-2xl p-6 max-w-lg mx-auto text-left space-y-3">
              <div className="flex items-center justify-between">
                <span className="text-xs text-slate-400 font-semibold uppercase">Overall Rating</span>
                <span className="text-base font-black text-amber-400 flex items-center gap-1">
                  <span>⭐</span>
                  <span>{liveAverage} / 5.0</span>
                </span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-xs text-slate-400 font-semibold uppercase">Your Verdict</span>
                <span className={`text-xs font-black uppercase px-2.5 py-0.5 rounded-full ${
                  feedbackForm.result === 'SELECTED'
                    ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/40'
                    : 'bg-rose-500/20 text-rose-300 border border-rose-500/40'
                }`}>
                  {feedbackForm.result === 'SELECTED' ? '✓ Good / Recommended to Advance' : '✕ Bad / Not Recommended'}
                </span>
              </div>
              <div className="pt-2 border-t border-slate-800 text-xs text-slate-300 italic">
                "{feedbackForm.comments}"
              </div>
            </div>

            <div className="flex items-center justify-center gap-4 pt-4">
              <button
                type="button"
                onClick={() => {
                  setSubmittedSuccess(false);
                  setSession(null);
                  setCurrentToken('');
                }}
                className="px-5 py-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-bold transition"
              >
                Evaluate Another Candidate
              </button>
              <button
                type="button"
                onClick={onSwitchToHR}
                className="px-6 py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-black shadow-lg shadow-emerald-600/30 transition flex items-center gap-1.5"
              >
                <span>View on HR Dashboard</span>
                <span>&rarr;</span>
              </button>
            </div>
          </div>
        ) : session ? (
          <div className="space-y-6">
            {/* Candidate Overview Card */}
            <div className="bg-slate-950 border border-slate-800 rounded-3xl p-6 sm:p-8 shadow-xl relative overflow-hidden">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-6 border-b border-slate-800/80">
                <div>
                  <div className="flex items-center gap-2">
                    <span className="text-xs font-bold text-amber-400 uppercase tracking-wider bg-amber-500/10 border border-amber-500/30 px-2.5 py-0.5 rounded-full">
                      {session.roundDisplayName}
                    </span>
                    {session.evaluated && (
                      <span className="text-xs font-bold text-emerald-400 bg-emerald-500/10 border border-emerald-500/30 px-2.5 py-0.5 rounded-full">
                        ✓ Feedback Recorded
                      </span>
                    )}
                  </div>
                  <h2 className="text-2xl sm:text-3xl font-black text-white mt-2">{session.candidateName}</h2>
                  <div className="text-xs sm:text-sm text-slate-400 mt-0.5">
                    Applying for: <strong className="text-slate-200">{session.jobTitle}</strong> ({session.department})
                  </div>
                </div>

                {session.atsScore != null && (
                  <div className="bg-slate-900 border border-slate-800 rounded-2xl p-3.5 text-center sm:text-right">
                    <div className="text-[10px] text-slate-400 font-bold uppercase tracking-wider">ATS Score</div>
                    <div className="text-2xl font-black text-emerald-400 mt-0.5">{session.atsScore} / 100</div>
                    <div className="text-[10px] text-emerald-500 font-semibold">Stage 1 Screened</div>
                  </div>
                )}
              </div>

              {/* Details */}
              <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 pt-5 text-xs">
                <div className="bg-slate-900/60 p-3 rounded-xl border border-slate-800/80">
                  <span className="text-slate-400 block mb-0.5">📅 Scheduled Slot:</span>
                  <strong className="text-slate-200">{session.interviewDate ? new Date(session.interviewDate).toLocaleString() : 'TBD'}</strong>
                </div>
                <div className="bg-slate-900/60 p-3 rounded-xl border border-slate-800/80">
                  <span className="text-slate-400 block mb-0.5">⏱️ Duration:</span>
                  <strong className="text-slate-200">{session.durationMinutes || 45} Minutes</strong>
                </div>
                <div className="bg-slate-900/60 p-3 rounded-xl border border-slate-800/80">
                  <span className="text-slate-400 block mb-0.5">👤 Assigned Panelist:</span>
                  <strong className="text-slate-200">{session.interviewerName}</strong>
                </div>
              </div>

              {/* Google Meet Button */}
              {session.meetingLink && (
                <div className="mt-5 p-4 rounded-2xl bg-gradient-to-r from-blue-950/80 to-indigo-950/80 border border-blue-800/60 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                  <div>
                    <div className="text-xs font-bold text-blue-300">Google Meet Meeting Room</div>
                    <div className="text-[11px] font-mono text-slate-400">{session.meetingLink}</div>
                  </div>
                  <a
                    href={session.meetingLink}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="px-5 py-2.5 bg-blue-600 hover:bg-blue-500 text-white rounded-xl text-xs font-bold shadow-md shadow-blue-600/30 flex items-center justify-center gap-1.5 transition whitespace-nowrap"
                  >
                    <span>🎥 Join Google Meet</span>
                    <span>&rarr;</span>
                  </a>
                </div>
              )}

              {/* Recruiter Focus Notes */}
              {session.notes && (
                <div className="mt-4 p-3 rounded-xl bg-amber-950/30 border border-amber-800/50 text-xs text-amber-200">
                  <strong className="text-amber-300 block mb-0.5">📌 Recruiter Notes & Focus Areas:</strong>
                  {session.notes}
                </div>
              )}
            </div>

            {/* Assessment Form */}
            <form onSubmit={handleSubmitFeedback} className="bg-slate-950 border border-slate-800 rounded-3xl p-6 sm:p-8 shadow-xl space-y-6">
              <div className="border-b border-slate-800 pb-4 flex items-center justify-between">
                <div>
                  <span className="text-xs font-bold text-amber-400 uppercase tracking-wider">Candidate Scorecard</span>
                  <h3 className="text-xl font-black text-white mt-0.5">Score the 4 Interview Parameters</h3>
                  <p className="text-xs text-slate-400 mt-1">Rate candidate performance on each criterion from 1 (Poor) to 5 (Exceptional)</p>
                </div>
                <div className="text-right">
                  <div className="text-xs font-bold text-slate-400">Overall Rating</div>
                  <div className="text-2xl font-black text-amber-400 mt-0.5">⭐ {liveAverage} / 5.0</div>
                </div>
              </div>

              {/* 4 Parameters */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
                <div className="p-4 rounded-2xl bg-slate-900/80 border border-slate-800 space-y-2">
                  <StarRating
                    label="1. Technical Competency"
                    value={feedbackForm.technicalRating}
                    onChange={v => setFeedbackForm({ ...feedbackForm, technicalRating: v })}
                    helperText="Depth of coding, system design, architectural principles, algorithms, and practical debugging."
                  />
                </div>
                <div className="p-4 rounded-2xl bg-slate-900/80 border border-slate-800 space-y-2">
                  <StarRating
                    label="2. Communication Skills"
                    value={feedbackForm.communicationRating}
                    onChange={v => setFeedbackForm({ ...feedbackForm, communicationRating: v })}
                    helperText="Clarity of expression, structure of thoughts, active listening, and explaining complex ideas."
                  />
                </div>
                <div className="p-4 rounded-2xl bg-slate-900/80 border border-slate-800 space-y-2">
                  <StarRating
                    label="3. Confidence Level"
                    value={feedbackForm.confidenceRating}
                    onChange={v => setFeedbackForm({ ...feedbackForm, confidenceRating: v })}
                    helperText="Poise under technical questioning, ownership, conviction, and composure when faced with unknown problems."
                  />
                </div>
                <div className="p-4 rounded-2xl bg-slate-900/80 border border-slate-800 space-y-2">
                  <StarRating
                    label="4. Body Language & Demeanor"
                    value={feedbackForm.bodyLanguageRating}
                    onChange={v => setFeedbackForm({ ...feedbackForm, bodyLanguageRating: v })}
                    helperText="Professional presence, video engagement, eye contact, respectful posture, and team enthusiasm."
                  />
                </div>
              </div>

              {/* Recommendation Verdict */}
              <div className="space-y-2 pt-2">
                <label className="block text-xs font-bold text-slate-300 uppercase tracking-wider">
                  Overall Recommendation Verdict *
                </label>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <button
                    type="button"
                    onClick={() => setFeedbackForm({ ...feedbackForm, result: 'SELECTED' })}
                    className={`p-4 rounded-2xl border text-left transition flex items-start gap-3.5 ${
                      feedbackForm.result === 'SELECTED'
                        ? 'bg-emerald-950/60 border-emerald-500 ring-2 ring-emerald-500/30'
                        : 'bg-slate-900/60 border-slate-800 hover:border-slate-700'
                    }`}
                  >
                    <span className="text-2xl mt-0.5">✅</span>
                    <div>
                      <div className="font-extrabold text-sm text-white">Good / Recommended to Advance (Pass)</div>
                      <div className="text-xs text-slate-400 mt-1">
                        Candidate meets technical & culture bar. Recommend passing to the next pipeline round.
                      </div>
                    </div>
                  </button>

                  <button
                    type="button"
                    onClick={() => setFeedbackForm({ ...feedbackForm, result: 'NOT_SELECTED' })}
                    className={`p-4 rounded-2xl border text-left transition flex items-start gap-3.5 ${
                      feedbackForm.result === 'NOT_SELECTED'
                        ? 'bg-rose-950/60 border-rose-500 ring-2 ring-rose-500/30'
                        : 'bg-slate-900/60 border-slate-800 hover:border-slate-700'
                    }`}
                  >
                    <span className="text-2xl mt-0.5">❌</span>
                    <div>
                      <div className="font-extrabold text-sm text-white">Bad / Not Recommended (Reject)</div>
                      <div className="text-xs text-slate-400 mt-1">
                        Candidate does not meet requirements for this opening. Halt pipeline and send regret notice.
                      </div>
                    </div>
                  </button>
                </div>
              </div>

              {/* Bottom Comment Box */}
              <div>
                <div className="flex items-center justify-between mb-1.5">
                  <label className="block text-xs font-bold text-slate-300 uppercase tracking-wider">
                    Interviewer Comments & Detailed Feedback *
                  </label>
                  <span className="text-[11px] text-slate-500">Reflects on HR Dashboard</span>
                </div>
                <textarea
                  rows="4"
                  required
                  value={feedbackForm.comments}
                  onChange={e => setFeedbackForm({ ...feedbackForm, comments: e.target.value })}
                  placeholder="Provide comprehensive observations: What were their core technical strengths? How did they approach problems? Any specific gaps or coaching areas? Cultural fit notes..."
                  className="w-full text-sm p-4 bg-slate-900 border border-slate-800 rounded-2xl text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-amber-500 leading-relaxed font-sans"
                ></textarea>
              </div>

              {/* Submit */}
              <div className="pt-4 border-t border-slate-800 flex items-center justify-between flex-wrap gap-4">
                <div className="text-xs text-slate-400">
                  Overall Score: <strong className="text-amber-400 font-bold">⭐ {liveAverage} / 5.0</strong>
                </div>

                <button
                  type="submit"
                  disabled={submitting}
                  className="px-8 py-3 bg-gradient-to-r from-emerald-600 via-teal-600 to-emerald-700 hover:from-emerald-500 hover:to-teal-500 text-white font-extrabold text-sm rounded-xl shadow-lg shadow-emerald-600/30 transition transform active:scale-95 disabled:opacity-50 flex items-center gap-2"
                >
                  <span>{submitting ? '⏳' : '🚀'}</span>
                  <span>{submitting ? 'Submitting Evaluation to HR...' : 'Submit Evaluation to HR Dashboard'}</span>
                </button>
              </div>
            </form>
          </div>
        ) : (
          <div className="space-y-6">
            {/* Header greeting & overview */}
            <div className="bg-slate-950 border border-slate-800 rounded-3xl p-6 sm:p-8 shadow-xl">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                <div>
                  <span className="text-xs uppercase font-bold text-amber-400 tracking-wider bg-amber-500/10 border border-amber-500/30 px-3 py-1 rounded-full">
                    ⭐ Panelist Workspace
                  </span>
                  <h2 className="text-2xl sm:text-3xl font-black text-white mt-3">
                    Welcome, {interviewer?.fullName || 'Panelist'}
                  </h2>
                  <p className="text-xs sm:text-sm text-slate-400 mt-1">
                    Here are your assigned candidate interviews. Join the Google Meet room to conduct the interview, then score the 4 parameters and submit your recommendation.
                  </p>
                </div>

                <div className="flex items-center gap-3">
                  <button
                    type="button"
                    onClick={() => {
                      if (interviewer?.email) fetchAssignedInterviews(interviewer.email);
                    }}
                    className="p-2.5 rounded-xl bg-slate-900 border border-slate-800 text-slate-300 hover:text-white transition"
                    title="Refresh Assigned Interviews"
                  >
                    🔄
                  </button>
                </div>
              </div>

              {/* Quick Metrics */}
              <div className="grid grid-cols-3 gap-3 sm:gap-4 mt-6 pt-6 border-t border-slate-800/80">
                <div className="bg-slate-900/80 p-3.5 rounded-2xl border border-slate-800">
                  <div className="text-[11px] text-slate-400 uppercase font-bold tracking-wider">Total Assigned</div>
                  <div className="text-2xl font-black text-white mt-1">{(assignedInterviews || []).length}</div>
                </div>
                <div className="bg-slate-900/80 p-3.5 rounded-2xl border border-slate-800">
                  <div className="text-[11px] text-amber-400 uppercase font-bold tracking-wider">Pending Evaluation</div>
                  <div className="text-2xl font-black text-amber-400 mt-1">{pendingInterviews.length}</div>
                </div>
                <div className="bg-slate-900/80 p-3.5 rounded-2xl border border-slate-800">
                  <div className="text-[11px] text-emerald-400 uppercase font-bold tracking-wider">Evaluated</div>
                  <div className="text-2xl font-black text-emerald-400 mt-1">{completedInterviews.length}</div>
                </div>
              </div>
            </div>

            {/* Section: Pending Interviews */}
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <h3 className="font-extrabold text-white text-lg flex items-center gap-2">
                  <span>📅</span>
                  <span>Scheduled Interviews to Conduct ({pendingInterviews.length})</span>
                </h3>
              </div>

              {pendingInterviews.length === 0 ? (
                <div className="bg-slate-950 border border-slate-800 rounded-3xl p-8 text-center text-slate-400">
                  <div className="text-3xl mb-2">🎉</div>
                  <div className="font-bold text-white text-sm">No Pending Interviews!</div>
                  <div className="text-xs text-slate-500 mt-1">
                    You have evaluated all currently assigned candidate rounds. New interviews scheduled by HR will appear here automatically.
                  </div>
                </div>
              ) : (
                <div className="grid grid-cols-1 gap-4">
                  {pendingInterviews.map((inv) => (
                    <div key={inv.interviewId || inv.id} className="bg-slate-950 border border-slate-800 hover:border-slate-700 rounded-3xl p-6 shadow-xl transition space-y-4">
                      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-4 border-b border-slate-800/80">
                        <div>
                          <div className="flex items-center gap-2">
                            <span className="text-xs font-bold text-amber-400 bg-amber-500/10 border border-amber-500/30 px-2.5 py-0.5 rounded-full">
                              {inv.roundDisplayName}
                            </span>
                            <span className="text-xs font-bold text-blue-400 bg-blue-500/10 border border-blue-500/30 px-2.5 py-0.5 rounded-full">
                              {inv.jobTitle}
                            </span>
                          </div>
                          <h4 className="text-xl font-black text-white mt-2">{inv.candidateName}</h4>
                          <div className="text-xs text-slate-400 mt-0.5 font-mono">{inv.candidateEmail}</div>
                        </div>

                        {inv.atsScore != null && (
                          <div className="bg-slate-900 border border-slate-800 rounded-2xl px-4 py-2 text-center sm:text-right shrink-0">
                            <div className="text-[10px] text-slate-400 font-bold uppercase">ATS Score</div>
                            <div className="text-lg font-black text-emerald-400">{inv.atsScore} / 100</div>
                          </div>
                        )}
                      </div>

                      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs">
                        <div className="bg-slate-900/60 p-3 rounded-xl border border-slate-800/80">
                          <span className="text-slate-400 block mb-0.5">📅 Scheduled Date & Time:</span>
                          <strong className="text-slate-200">{inv.interviewDate ? new Date(inv.interviewDate).toLocaleString() : 'TBD'}</strong>
                        </div>
                        <div className="bg-slate-900/60 p-3 rounded-xl border border-slate-800/80">
                          <span className="text-slate-400 block mb-0.5">⏱️ Duration:</span>
                          <strong className="text-slate-200">{inv.durationMinutes || 45} Minutes</strong>
                        </div>
                      </div>

                      {inv.notes && (
                        <div className="p-3 rounded-xl bg-amber-950/20 border border-amber-800/30 text-xs text-amber-200/90">
                          <strong>📌 Agenda Notes:</strong> {inv.notes}
                        </div>
                      )}

                      <div className="pt-2 flex flex-wrap items-center justify-between gap-3">
                        {inv.meetingLink ? (
                          <a
                            href={inv.meetingLink}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="px-5 py-2.5 rounded-xl bg-blue-600 hover:bg-blue-500 text-white font-bold text-xs shadow-md shadow-blue-600/30 flex items-center gap-1.5 transition"
                          >
                            <span>🎥 Join Google Meet</span>
                            <span>&rarr;</span>
                          </a>
                        ) : (
                          <span className="text-xs text-slate-500">Google Meet link pending</span>
                        )}

                        <button
                          type="button"
                          onClick={() => {
                            setSession(inv);
                            setFeedbackForm({
                              technicalRating: inv.technicalRating || 5,
                              communicationRating: inv.communicationRating || 4,
                              confidenceRating: inv.confidenceRating || 4,
                              bodyLanguageRating: inv.bodyLanguageRating || 5,
                              result: inv.result === 'NOT_SELECTED' ? 'NOT_SELECTED' : 'SELECTED',
                              comments: inv.comments || '',
                              interviewerName: interviewer?.fullName || inv.interviewerName || ''
                            });
                          }}
                          className="px-6 py-2.5 rounded-xl bg-gradient-to-r from-amber-500 to-orange-500 hover:from-amber-400 hover:to-orange-400 text-slate-950 font-black text-xs shadow-md shadow-amber-500/20 transition flex items-center gap-1.5"
                        >
                          <span>⭐ Start Candidate Evaluation</span>
                          <span>&rarr;</span>
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>

            {/* Section: Completed Evaluations History */}
            {completedInterviews.length > 0 && (
              <div className="space-y-4 pt-6 border-t border-slate-800">
                <h3 className="font-extrabold text-white text-lg flex items-center gap-2">
                  <span>✅</span>
                  <span>My Completed Candidate Evaluations ({completedInterviews.length})</span>
                </h3>

                <div className="bg-slate-950 border border-slate-800 rounded-3xl p-6 shadow-xl overflow-hidden divide-y divide-slate-800">
                  {completedInterviews.map((inv) => (
                    <div key={inv.interviewId || inv.id} className="py-4 space-y-2 first:pt-0 last:pb-0">
                      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                        <div>
                          <div className="font-bold text-white text-base">{inv.candidateName}</div>
                          <div className="text-xs text-slate-400">
                            {inv.roundDisplayName} &bull; {inv.jobTitle} &bull; Evaluated on {inv.evaluatedAt ? new Date(inv.evaluatedAt).toLocaleDateString() : 'Recent'}
                          </div>
                        </div>
                        <div className="flex items-center gap-3">
                          <span className="font-black text-amber-400 text-sm">
                            ⭐ {inv.overallRating ? inv.overallRating.toFixed(1) : '4.5'} / 5.0
                          </span>
                          <span className={`text-xs font-black uppercase px-2.5 py-0.5 rounded-full ${
                            inv.result === 'SELECTED'
                              ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/30'
                              : 'bg-rose-500/20 text-rose-300 border border-rose-500/30'
                          }`}>
                            {inv.result === 'SELECTED' ? 'Pass (Selected)' : 'Not Selected'}
                          </span>
                        </div>
                      </div>

                      {/* 4 Ratings Pills */}
                      <div className="flex flex-wrap gap-2 text-[11px] pt-1">
                        <span className="px-2.5 py-0.5 rounded-lg bg-slate-900 border border-slate-800 text-slate-300">
                          Tech: <strong className="text-amber-400">{inv.technicalRating || 5}/5</strong>
                        </span>
                        <span className="px-2.5 py-0.5 rounded-lg bg-slate-900 border border-slate-800 text-slate-300">
                          Comm: <strong className="text-amber-400">{inv.communicationRating || 4}/5</strong>
                        </span>
                        <span className="px-2.5 py-0.5 rounded-lg bg-slate-900 border border-slate-800 text-slate-300">
                          Conf: <strong className="text-amber-400">{inv.confidenceRating || 4}/5</strong>
                        </span>
                        <span className="px-2.5 py-0.5 rounded-lg bg-slate-900 border border-slate-800 text-slate-300">
                          Body: <strong className="text-amber-400">{inv.bodyLanguageRating || 5}/5</strong>
                        </span>
                      </div>

                      {inv.comments && (
                        <div className="text-xs text-slate-400 italic bg-slate-900/60 p-3 rounded-xl border border-slate-800/60">
                          "{inv.comments}"
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        )}
      </main>
    </div>
  );
}

export function RecordDecisionModal({ candidate, interview, form, setForm, submitting, onClose, onSubmit }) {
  const roundName = interview?.roundDisplayName || (form.round ? formatRoundName(form.round) : 'Current Round');
  const isHRRound = interview?.round === 'HR_ROUND' || form.round === 'HR_ROUND';

  return (
    <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="w-full max-w-xl bg-white rounded-3xl shadow-2xl overflow-hidden">
        {/* Header */}
        <div className="p-6 bg-gradient-to-r from-indigo-900 via-purple-900 to-slate-900 text-white flex items-center justify-between">
          <div>
            <span className="text-xs uppercase tracking-wider font-bold text-indigo-400">Interview Evaluation</span>
            <h3 className="font-black text-lg mt-0.5">Record Decision: {candidate.fullName}</h3>
            <div className="text-xs text-slate-300">Evaluating: <strong className="text-white">{roundName}</strong></div>
          </div>
          <button onClick={onClose} className="p-2 text-slate-400 hover:text-white text-xl font-bold">&times;</button>
        </div>

        <div className="p-6 space-y-4 max-h-[75vh] overflow-y-auto custom-scrollbar">
          {/* Active Interview details summary */}
          {interview && (
            <div className="p-3.5 rounded-xl bg-slate-50 border border-slate-200 text-xs space-y-1">
              <div className="flex items-center justify-between">
                <span className="text-slate-500">Scheduled Date:</span>
                <span className="font-semibold text-slate-800">{new Date(interview.interviewDate).toLocaleString()}</span>
              </div>
              {interview.meetingLink && (
                <div className="flex items-center justify-between">
                  <span className="text-slate-500">Google Meet:</span>
                  <a href={interview.meetingLink} target="_blank" rel="noopener noreferrer" className="font-mono text-blue-600 hover:underline">
                    {interview.meetingLink}
                  </a>
                </div>
              )}
            </div>
          )}

          {/* Decision Buttons */}
          <div>
            <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-2">Round Verdict *</label>
            <div className="grid grid-cols-2 gap-3">
              <button
                type="button"
                onClick={() => setForm({ ...form, result: 'SELECTED' })}
                className={`p-4 rounded-2xl border text-left transition ${
                  form.result === 'SELECTED'
                    ? 'border-emerald-500 bg-emerald-50 ring-2 ring-emerald-500/30'
                    : 'border-slate-200 hover:border-slate-300 bg-white'
                }`}
              >
                <div className="flex items-center justify-between">
                  <span className="text-xl">✅</span>
                  {form.result === 'SELECTED' && <span className="text-emerald-700 font-extrabold text-xs">SELECTED</span>}
                </div>
                <div className="font-bold text-sm text-slate-900 mt-1">
                  {isHRRound ? 'PASS & EXTEND OFFER' : 'PASS & ADVANCE ROUND'}
                </div>
                <div className="text-[11px] text-slate-500 mt-1">
                  {isHRRound
                    ? 'Marks candidate as HIRED and triggers official Offer email!'
                    : 'Advances candidate to the next round in the pipeline.'}
                </div>
              </button>

              <button
                type="button"
                onClick={() => setForm({ ...form, result: 'NOT_SELECTED' })}
                className={`p-4 rounded-2xl border text-left transition ${
                  form.result === 'NOT_SELECTED'
                    ? 'border-rose-500 bg-rose-50 ring-2 ring-rose-500/30'
                    : 'border-slate-200 hover:border-slate-300 bg-white'
                }`}
              >
                <div className="flex items-center justify-between">
                  <span className="text-xl">❌</span>
                  {form.result === 'NOT_SELECTED' && <span className="text-rose-700 font-extrabold text-xs">REJECTED</span>}
                </div>
                <div className="font-bold text-sm text-slate-900 mt-1">NOT SELECTED</div>
                <div className="text-[11px] text-slate-500 mt-1">
                  Halts pipeline and dispatches a graceful regret email to candidate.
                </div>
              </button>
            </div>
          </div>

          {/* Evaluator Name */}
          <div>
            <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1">Evaluator / Interviewer Name</label>
            <input
              type="text"
              value={form.interviewerName}
              onChange={e => setForm({ ...form, interviewerName: e.target.value })}
              className="w-full text-sm px-3.5 py-2.5 border border-slate-200 rounded-xl focus:ring-2 focus:ring-indigo-500 focus:outline-none"
            />
          </div>

          {/* Feedback & Notes */}
          <div>
            <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1">
              Feedback & Evaluation Notes *
            </label>
            <textarea
              rows="3"
              value={form.feedback}
              onChange={e => setForm({ ...form, feedback: e.target.value })}
              placeholder="Provide technical feedback, strengths, and areas for improvement..."
              className="w-full text-sm px-3.5 py-2.5 border border-slate-200 rounded-xl focus:ring-2 focus:ring-indigo-500 focus:outline-none"
            ></textarea>
          </div>

          {/* Footer Buttons */}
          <div className="pt-2 flex items-center justify-end gap-3 border-t border-slate-100">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2.5 rounded-xl border border-slate-200 text-slate-700 text-sm font-semibold hover:bg-slate-50"
            >
              Cancel
            </button>
            <button
              type="button"
              onClick={onSubmit}
              disabled={submitting}
              className="px-6 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white text-sm font-bold shadow-lg shadow-indigo-600/30 disabled:opacity-60 flex items-center gap-2"
            >
              <span>{submitting ? '⏳' : '💾'}</span>
              <span>{submitting ? 'Recording Decision & Sending Email...' : 'Save Decision & Send Email'}</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

export function InterviewDecisionModal({ candidate, form, setForm, submitting, onClose, onSubmit }) {
  return (
    <RecordDecisionModal
      candidate={candidate}
      interview={null}
      form={form}
      setForm={setForm}
      submitting={submitting}
      onClose={onClose}
      onSubmit={onSubmit}
    />
  );
}

function CreateJobDialog({ form, setForm, onClose, onSubmit }) {
  return (
    <div className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="w-full max-w-xl bg-white rounded-3xl shadow-2xl p-6">
        <h3 className="font-bold text-lg mb-4">Create Job Opening</h3>
        <div className="space-y-3">
          <input
            type="text"
            value={form.jobTitle}
            onChange={e => setForm({ ...form, jobTitle: e.target.value })}
            placeholder="Job Title"
            className="w-full text-sm px-3.5 py-2 border rounded-xl"
          />
          <input
            type="text"
            value={form.requiredSkills}
            onChange={e => setForm({ ...form, requiredSkills: e.target.value })}
            placeholder="Required Skills (comma-separated)"
            className="w-full text-sm px-3.5 py-2 border rounded-xl"
          />
        </div>
        <div className="flex justify-end gap-3 mt-4">
          <button onClick={onClose} className="px-4 py-2 border rounded-xl">Cancel</button>
          <button onClick={onSubmit} className="px-5 py-2 bg-blue-600 text-white font-bold rounded-xl">Publish Job</button>
        </div>
      </div>
    </div>
  );
}

function renderStatusBadge(status) {
  const map = {
    SHORTLISTED: { bg: 'bg-emerald-100 text-emerald-800 border-emerald-300', label: 'SHORTLISTED' },
    L1_SCHEDULED: { bg: 'bg-blue-100 text-blue-800 border-blue-300', label: 'L1 SCHEDULED' },
    L1_PASSED: { bg: 'bg-teal-100 text-teal-800 border-teal-300', label: 'L1 PASSED' },
    L2_SCHEDULED: { bg: 'bg-indigo-100 text-indigo-800 border-indigo-300', label: 'L2 SCHEDULED' },
    L2_PASSED: { bg: 'bg-cyan-100 text-cyan-800 border-cyan-300', label: 'L2 PASSED' },
    MANAGERIAL_SCHEDULED: { bg: 'bg-violet-100 text-violet-800 border-violet-300', label: 'MANAGERIAL SCHEDULED' },
    MANAGERIAL_PASSED: { bg: 'bg-purple-100 text-purple-800 border-purple-300', label: 'MANAGERIAL PASSED' },
    HR_SCHEDULED: { bg: 'bg-fuchsia-100 text-fuchsia-800 border-fuchsia-300', label: 'HR ROUND SCHEDULED' },
    SELECTED: { bg: 'bg-emerald-600 text-white border-emerald-700', label: '🎉 SELECTED / HIRED' },
    NOT_SELECTED: { bg: 'bg-rose-100 text-rose-800 border-rose-300', label: 'NOT SELECTED' },
    REJECTED: { bg: 'bg-rose-100 text-rose-800 border-rose-300', label: 'REJECTED' },
    REVIEW_REQUIRED: { bg: 'bg-amber-100 text-amber-800 border-amber-300', label: 'REVIEW REQUIRED' },
    INTERVIEW_SCHEDULED: { bg: 'bg-blue-100 text-blue-800 border-blue-300', label: 'INTERVIEW SCHEDULED' },
    SCREENING: { bg: 'bg-sky-100 text-sky-800 border-sky-300', label: 'SCREENING' },
    RECEIVED: { bg: 'bg-slate-100 text-slate-700 border-slate-300', label: 'RECEIVED' },
    PARSED: { bg: 'bg-slate-100 text-slate-700 border-slate-300', label: 'PARSED' }
  };
  const conf = map[status] || { bg: 'bg-slate-100 text-slate-700 border-slate-200', label: status || 'PENDING' };
  return <span className={`inline-block px-2.5 py-1 rounded-full text-xs font-bold border ${conf.bg}`}>{conf.label}</span>;
}

function renderGenderBadge(gender) {
  if (!gender || gender === 'UNSPECIFIED') {
    return (
      <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md text-[11px] font-semibold bg-slate-100 text-slate-600 border border-slate-200">
        <span>👤</span>
        <span>Unspecified</span>
      </span>
    );
  }
  if (gender === 'MALE') {
    return (
      <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md text-[11px] font-bold bg-blue-50 text-blue-700 border border-blue-200">
        <span>👨</span>
        <span>Male</span>
      </span>
    );
  }
  if (gender === 'FEMALE') {
    return (
      <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md text-[11px] font-bold bg-pink-50 text-pink-700 border border-pink-200">
        <span>👩</span>
        <span>Female</span>
      </span>
    );
  }
  if (gender === 'OTHER') {
    return (
      <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md text-[11px] font-bold bg-purple-50 text-purple-700 border border-purple-200">
        <span>⚧</span>
        <span>Other</span>
      </span>
    );
  }
  return null;
}

function renderScoreBadge(score) {
  if (score === null || score === undefined) return <span className="text-slate-400 font-mono text-xs">--</span>;
  const num = parseFloat(score);
  let color = 'text-rose-600 bg-rose-50 border-rose-200';
  if (num >= 80) color = 'text-emerald-700 bg-emerald-50 border-emerald-200';
  else if (num >= 60) color = 'text-amber-700 bg-amber-50 border-amber-200';
  return <span className={`px-2.5 py-0.5 rounded-md text-xs font-mono font-bold border ${color}`}>{num.toFixed(1)} / 100</span>;
}

// ==========================================
// INTERVIEWER LOGIN PAGE (DEDICATED PORTAL)
// ==========================================
export function InterviewerLoginPage({ onLogin, onSwitchToHR, toast, setToast }) {
  const [email, setEmail] = useState('lead.architect@sanvii.com');
  const [password, setPassword] = useState('Sanvii@2016');
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const handleSubmit = async (e) => {
    if (e) e.preventDefault();
    setError(null);
    if (!email.trim() || !password.trim()) {
      setError('Please enter both your work email and password.');
      return;
    }

    setLoading(true);
    try {
      const res = await fetch(`${API_BASE}/api/v1/interviewer/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email: email.trim(), password: password.trim() })
      });
      const json = await res.json();
      if (res.ok && json.success) {
        onLogin(json.data);
      } else {
        setError(json.message || 'Invalid interviewer credentials. Please check your credentials with your HR Administrator.');
      }
    } catch (err) {
      setError('Login connection error: ' + err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col justify-center items-center px-4 py-12 relative overflow-hidden">
      <div className="absolute top-1/4 -left-32 w-96 h-96 bg-amber-600/15 rounded-full blur-3xl pointer-events-none"></div>
      <div className="absolute bottom-1/4 -right-32 w-96 h-96 bg-orange-600/15 rounded-full blur-3xl pointer-events-none"></div>

      {toast && (
        <div className={`fixed top-6 right-6 z-50 px-5 py-3.5 rounded-xl shadow-2xl border text-sm font-medium flex items-center gap-3 transition-all ${
          toast.type === 'error' ? 'bg-red-50 text-red-900 border-red-300' :
          toast.type === 'success' ? 'bg-emerald-50 text-emerald-900 border-emerald-300' :
          'bg-blue-50 text-blue-900 border-blue-300'
        }`}>
          <span>{toast.type === 'error' ? '⚠️' : toast.type === 'success' ? '✅' : 'ℹ️'}</span>
          <span>{toast.message}</span>
          <button onClick={() => setToast(null)} className="ml-2 text-slate-400 hover:text-slate-600 font-bold">&times;</button>
        </div>
      )}

      <div className="w-full max-w-md relative z-10">
        {/* Brand Logo & Header */}
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center bg-white rounded-2xl px-6 py-3.5 shadow-2xl shadow-amber-500/20 mb-4 ring-4 ring-amber-500/30 max-w-[280px]">
            <img
              src="/sanvii-logo.png"
              alt="Sanvii Techmet"
              className="h-12 w-auto object-contain"
              onError={(e) => { if (window.SANVII_LOGO_DATA) e.target.src = window.SANVII_LOGO_DATA; }}
            />
          </div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight">
            Sanvii ATS Interviewer Portal
          </h1>
          <p className="text-xs sm:text-sm text-slate-400 mt-1 font-medium">
            SANVII TECHMET &bull; Panelist Evaluation & Scoring Engine
          </p>
          
          <div className="mt-3 inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-amber-500/15 border border-amber-500/30 text-amber-300 text-xs font-semibold">
            <span>⭐</span>
            <span>Interviewer Panelist Access</span>
          </div>
        </div>

        {/* Login Card */}
        <div className="bg-slate-900/90 backdrop-blur-xl border border-slate-800 rounded-3xl p-6 sm:p-8 shadow-2xl shadow-black/70">
          <div className="mb-6">
            <h2 className="text-lg font-bold text-slate-100 flex items-center gap-2">
              <span>Panelist Sign In</span>
              <span className="text-xs font-normal text-amber-400 bg-amber-500/10 px-2 py-0.5 rounded border border-amber-500/20">Interviewer</span>
            </h2>
            <p className="text-xs text-slate-400 mt-1.5 leading-relaxed">
              Sign in using the interviewer credentials provided by HR to access your assigned candidate interviews and submit evaluations.
            </p>
          </div>

          {error && (
            <div className="mb-5 p-3.5 rounded-xl bg-red-950/60 border border-red-800 text-red-300 text-xs flex items-start gap-2.5">
              <span className="text-base leading-none">⚠️</span>
              <span className="leading-relaxed font-medium">{error}</span>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
                Interviewer Work Email
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-500">
                  <span>✉️</span>
                </div>
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="e.g. lead.architect@sanvii.com"
                  className="w-full pl-10 pr-4 py-2.5 bg-slate-950 border border-slate-800 focus:border-amber-500 focus:ring-2 focus:ring-amber-500/20 rounded-xl text-sm text-white placeholder-slate-500 transition outline-none font-medium"
                  required
                />
              </div>
            </div>

            <div>
              <div className="flex items-center justify-between mb-1.5">
                <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider">
                  Security Password
                </label>
                <span className="text-[11px] text-amber-400 font-mono">HR-Provided</span>
              </div>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-500">
                  <span>🔒</span>
                </div>
                <input
                  type={showPassword ? 'text' : 'password'}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="Enter your interviewer password"
                  className="w-full pl-10 pr-10 py-2.5 bg-slate-950 border border-slate-800 focus:border-amber-500 focus:ring-2 focus:ring-amber-500/20 rounded-xl text-sm text-white placeholder-slate-500 transition outline-none font-medium"
                  required
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute inset-y-0 right-0 pr-3.5 flex items-center text-slate-400 hover:text-slate-200 transition text-sm"
                  title={showPassword ? 'Hide password' : 'Show password'}
                >
                  {showPassword ? '👁️' : '🙈'}
                </button>
              </div>
            </div>

            <div className="pt-2">
              <button
                type="submit"
                disabled={loading}
                className="w-full py-3 px-4 bg-gradient-to-r from-amber-500 via-orange-500 to-amber-600 hover:from-amber-400 hover:to-orange-400 text-slate-950 font-black rounded-xl shadow-lg shadow-amber-500/20 transition transform active:scale-[0.99] disabled:opacity-50 flex items-center justify-center gap-2 text-sm"
              >
                {loading ? (
                  <>
                    <span className="w-4 h-4 border-2 border-slate-950/30 border-t-slate-950 rounded-full animate-spin"></span>
                    <span>Authenticating Interviewer...</span>
                  </>
                ) : (
                  <>
                    <span>Sign In to Interviewer Workspace</span>
                    <span>➔</span>
                  </>
                )}
              </button>
            </div>
          </form>

          {/* Quick Fill Sample Interviewer */}
          <div className="mt-6 pt-5 border-t border-slate-800/80">
            <div className="bg-slate-950/70 border border-slate-800/80 rounded-2xl p-3.5 text-xs">
              <div className="flex items-center justify-between mb-2">
                <span className="font-semibold text-slate-300 flex items-center gap-1.5">
                  <span>🔑</span>
                  <span>Sample Interviewer Credentials</span>
                </span>
                <button
                  type="button"
                  onClick={() => {
                    setEmail('lead.architect@sanvii.com');
                    setPassword('Sanvii@2016');
                  }}
                  className="text-[11px] font-semibold text-amber-400 hover:text-amber-300 underline"
                >
                  Quick Fill
                </button>
              </div>
              <div className="space-y-1 font-mono text-[11px] text-slate-400">
                <div className="flex justify-between">
                  <span className="text-slate-500">Email:</span>
                  <span className="text-slate-200 font-bold">lead.architect@sanvii.com</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-500">Password:</span>
                  <span className="text-slate-200 font-bold">Sanvii@2016</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-500">Designation:</span>
                  <span className="text-amber-400 font-bold">Lead Technical Architect</span>
                </div>
              </div>
            </div>
          </div>

          {/* Switch to HR Portal */}
          <div className="mt-4 pt-4 border-t border-slate-800 text-center">
            <button
              type="button"
              onClick={onSwitchToHR}
              className="w-full py-2.5 px-4 bg-slate-800/80 hover:bg-slate-800 border border-slate-700/80 rounded-xl text-slate-300 hover:text-white text-xs font-bold transition flex items-center justify-center gap-2"
            >
              <span>🏛️ Are you an HR Administrator? Switch to HR Admin Portal &rarr;</span>
            </button>
          </div>
        </div>

        <div className="text-center mt-6 text-xs text-slate-500">
          <p>Sanvii ATS &bull; Role-Based Panelist Access Control &bull; Sanvii Techmet</p>
        </div>
      </div>
    </div>
  );
}

// ==========================================
// HR ADMINISTRATOR LOGIN PAGE
// ==========================================
export function LoginPage({ onLogin, toast, setToast }) {
  const [hrUsername, setHrUsername] = useState('Sanvii');
  const [hrPassword, setHrPassword] = useState('Sanvii@2016');
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleHrSubmit = async (e) => {
    if (e) e.preventDefault();
    setError('');
    if (!hrUsername.trim() || !hrPassword.trim()) {
      setError('Please enter both HR Admin username and password.');
      return;
    }

    setLoading(true);
    try {
      const testToken = btoa(`${hrUsername.trim()}:${hrPassword}`);
      const res = await fetch(`${API_BASE}/api/v1/auth/login`, {
        method: 'POST',
        headers: {
          'Authorization': `Basic ${testToken}`,
          'Content-Type': 'application/json'
        }
      });

      if (res.ok) {
        onLogin({
          username: hrUsername.trim(),
          password: hrPassword,
          role: 'HR Administrator'
        });
      } else {
        setError('Access Denied: Invalid HR Admin credentials. Only authorized HR Administrator (Sanvii) is permitted.');
      }
    } catch (err) {
      try {
        const testToken = btoa(`${hrUsername.trim()}:${hrPassword}`);
        const res2 = await fetch(`${API_BASE}/api/v1/dashboard/metrics`, {
          headers: { 'Authorization': `Basic ${testToken}` }
        });
        if (res2.ok) {
          onLogin({
            username: hrUsername.trim(),
            password: hrPassword,
            role: 'HR Administrator'
          });
          return;
        }
      } catch(e) {}
      setError('Authentication failed. Please check your credentials and server connection.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col justify-center items-center px-4 py-12 relative overflow-hidden">
      {/* Subtle glowing ambient lights */}
      <div className="absolute top-1/4 -left-32 w-96 h-96 bg-blue-600/15 rounded-full blur-3xl pointer-events-none"></div>
      <div className="absolute bottom-1/4 -right-32 w-96 h-96 bg-indigo-600/15 rounded-full blur-3xl pointer-events-none"></div>

      {/* Toast Notification */}
      {toast && (
        <div className={`fixed top-6 right-6 z-50 px-5 py-3.5 rounded-xl shadow-2xl border text-sm font-medium flex items-center gap-3 transition-all ${
          toast.type === 'error' ? 'bg-red-50 text-red-900 border-red-300' :
          toast.type === 'success' ? 'bg-emerald-50 text-emerald-900 border-emerald-300' :
          'bg-blue-50 text-blue-900 border-blue-300'
        }`}>
          <span>{toast.type === 'error' ? '⚠️' : toast.type === 'success' ? '✅' : 'ℹ️'}</span>
          <span>{toast.message}</span>
          <button onClick={() => setToast(null)} className="ml-2 text-slate-400 hover:text-slate-600 font-bold">&times;</button>
        </div>
      )}

      <div className="w-full max-w-md relative z-10">
        {/* Brand Logo & Header */}
        <div className="text-center mb-6">
          <div className="inline-flex items-center justify-center bg-white rounded-2xl px-6 py-3.5 shadow-2xl shadow-blue-500/20 mb-4 ring-4 ring-blue-500/30 max-w-[280px]">
            <img
              src="/sanvii-logo.png"
              alt="Sanvii Techmet"
              className="h-12 w-auto object-contain"
              onError={(e) => { if (window.SANVII_LOGO_DATA) e.target.src = window.SANVII_LOGO_DATA; }}
            />
          </div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight">
            Sanvii ATS Platform
          </h1>
          <p className="text-xs sm:text-sm text-slate-400 mt-1 font-medium">
            SANVII TECHMET &bull; Deploying Excellence, Delivering Success
          </p>
          <div className="mt-2.5 inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-blue-500/15 border border-blue-500/30 text-blue-300 text-xs font-semibold">
            <span>🏛️</span>
            <span>Authorized HR Administrator Access Only</span>
          </div>
        </div>

        {/* Login Card */}
        <div className="bg-slate-900/90 backdrop-blur-xl border border-slate-800 rounded-3xl p-6 sm:p-8 shadow-2xl shadow-black/70">
          <div className="mb-6">
            <h2 className="text-lg font-bold text-slate-100 flex items-center gap-2">
              <span>HR Admin Portal</span>
              <span className="text-xs font-normal text-indigo-400 bg-indigo-500/10 px-2 py-0.5 rounded border border-indigo-500/20">Admin Login</span>
            </h2>
            <p className="text-xs text-slate-400 mt-1.5 leading-relaxed">
              Enter authorized HR Administrator credentials to access candidate scoring, pipeline management, and panelist assignment.
            </p>
          </div>

          {error && (
            <div className="mb-5 p-3.5 rounded-xl bg-red-950/60 border border-red-800 text-red-300 text-xs flex items-start gap-2.5">
              <span className="text-base leading-none">⚠️</span>
              <span className="leading-relaxed font-medium">{error}</span>
            </div>
          )}

          <form onSubmit={handleHrSubmit} className="space-y-4">
            <div>
              <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider mb-1.5">
                HR Admin Username
              </label>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-500">
                  <span>👤</span>
                </div>
                <input
                  type="text"
                  value={hrUsername}
                  onChange={(e) => setHrUsername(e.target.value)}
                  placeholder="Enter username (Sanvii)"
                  className="w-full pl-10 pr-4 py-2.5 bg-slate-950 border border-slate-800 focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20 rounded-xl text-sm text-white placeholder-slate-500 transition outline-none font-medium"
                  required
                />
              </div>
            </div>

            <div>
              <div className="flex items-center justify-between mb-1.5">
                <label className="block text-xs font-semibold text-slate-300 uppercase tracking-wider">
                  Security Password
                </label>
                <span className="text-[11px] text-indigo-400 font-mono">Confidential</span>
              </div>
              <div className="relative">
                <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-500">
                  <span>🔒</span>
                </div>
                <input
                  type={showPassword ? 'text' : 'password'}
                  value={hrPassword}
                  onChange={(e) => setHrPassword(e.target.value)}
                  placeholder="Enter password (Sanvii@2016)"
                  className="w-full pl-10 pr-10 py-2.5 bg-slate-950 border border-slate-800 focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20 rounded-xl text-sm text-white placeholder-slate-500 transition outline-none font-medium"
                  required
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute inset-y-0 right-0 pr-3.5 flex items-center text-slate-400 hover:text-slate-200 transition text-sm"
                  title={showPassword ? 'Hide password' : 'Show password'}
                >
                  {showPassword ? '👁️' : '🙈'}
                </button>
              </div>
            </div>

            <div className="pt-2">
              <button
                type="submit"
                disabled={loading}
                className="w-full py-3 px-4 bg-gradient-to-r from-blue-600 via-indigo-600 to-blue-700 hover:from-blue-500 hover:to-indigo-600 text-white font-bold rounded-xl shadow-lg shadow-blue-600/30 transition transform active:scale-[0.99] disabled:opacity-50 flex items-center justify-center gap-2 text-sm"
              >
                {loading ? (
                  <>
                    <span className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin"></span>
                    <span>Authenticating HR Admin...</span>
                  </>
                ) : (
                  <>
                    <span>Sign In as HR Admin</span>
                    <span>➔</span>
                  </>
                )}
              </button>
            </div>
          </form>

          {/* Quick Fill HR Credentials */}
          <div className="mt-6 pt-5 border-t border-slate-800/80">
            <div className="bg-slate-950/70 border border-slate-800/80 rounded-2xl p-3.5 text-xs">
              <div className="flex items-center justify-between mb-2">
                <span className="font-semibold text-slate-300 flex items-center gap-1.5">
                  <span>🔑</span>
                  <span>Authorized HR Administrator</span>
                </span>
                <button
                  type="button"
                  onClick={() => {
                    setHrUsername('Sanvii');
                    setHrPassword('Sanvii@2016');
                  }}
                  className="text-[11px] font-semibold text-blue-400 hover:text-blue-300 underline"
                >
                  Quick Fill
                </button>
              </div>
              <div className="space-y-1 font-mono text-[11px] text-slate-400">
                <div className="flex justify-between">
                  <span className="text-slate-500">Username:</span>
                  <span className="text-slate-200 font-bold">Sanvii</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-slate-500">Password:</span>
                  <span className="text-slate-200 font-bold">Sanvii@2016</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Security note footer */}
        <div className="text-center mt-6 text-xs text-slate-500">
          <p>Sanvii ATS &bull; Role-Based Access Control Active &bull; Spring Security 6</p>
        </div>
      </div>
    </div>
  );
}

