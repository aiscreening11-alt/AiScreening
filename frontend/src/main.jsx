import React from 'react';
import ReactDOM from 'react-dom/client';
import App from './App.jsx';
import InterviewerApp from './InterviewerApp.jsx';
import './index.css';

// Dedicated route detection: If URL path is /interviewer or ?portal=interviewer, render InterviewerApp
const isInterviewerRoute = () => {
  try {
    const pathname = window.location.pathname.toLowerCase();
    const params = new URLSearchParams(window.location.search);
    return pathname.startsWith('/interviewer') || params.get('portal') === 'interviewer';
  } catch (e) {
    return false;
  }
};

const RootComponent = isInterviewerRoute() ? InterviewerApp : App;

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <RootComponent />
  </React.StrictMode>
);
