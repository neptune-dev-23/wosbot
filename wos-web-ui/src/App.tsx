import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";

import DashboardLayout from "./layouts/DashboardLayout";
import LogsPage from "./pages/LogsPage";
import ProfilesPage from "./pages/ProfilesPage";
import TasksPage from "./pages/TasksPage";
import LandingPage from "./pages/LandingPage";

const App = () => (
  <BrowserRouter>
    <Routes>
      <Route path="/" element={<LandingPage />} />
      <Route element={<DashboardLayout />}>
        <Route path="/logs" element={<LogsPage />} />
        <Route path="/profiles" element={<ProfilesPage />} />
        <Route path="/tasks" element={<TasksPage />} />
        <Route path="*" element={<Navigate to="/logs" replace />} />
      </Route>
    </Routes>
  </BrowserRouter>
);

export default App;
