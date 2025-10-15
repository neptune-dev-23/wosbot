import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";

import DashboardLayout from "./layouts/DashboardLayout";
import LogsPage from "./pages/LogsPage";
import ProfilesPage from "./pages/ProfilesPage";
import TasksPage from "./pages/TasksPage";
import TaskStatsPage from "./pages/TaskStatsPage";
import LandingPage from "./pages/LandingPage";
import AlliancePage from "./pages/AlliancePage";
import CityPage from "./pages/CityPage";
import EventsPage from "./pages/EventsPage";
import GatherPage from "./pages/GatherPage";
import IntelPage from "./pages/IntelPage";
import MobilizationPage from "./pages/MobilizationPage";
import PetsPage from "./pages/PetsPage";
import ShopPage from "./pages/ShopPage";
import TrainingPage from "./pages/TrainingPage";
import PolarTerrorPage from "./pages/PolarTerrorPage";
import BearTrapPage from "./pages/BearTrapPage";
import ChiefOrderPage from "./pages/ChiefOrderPage";
import ExpertsPage from "./pages/ExpertsPage";
import EmulatorPage from "./pages/EmulatorPage";

const App = () => (
  <BrowserRouter>
    <Routes>
      <Route path="/" element={<LandingPage />} />
      <Route element={<DashboardLayout />}>
        <Route path="/logs" element={<LogsPage />} />
        <Route path="/profiles" element={<ProfilesPage />} />
        <Route path="/tasks" element={<TasksPage />} />
        <Route path="/task-stats" element={<TaskStatsPage />} />
        <Route path="/alliance" element={<AlliancePage />} />
        <Route path="/city" element={<CityPage />} />
        <Route path="/events" element={<EventsPage />} />
        <Route path="/gather" element={<GatherPage />} />
        <Route path="/intel" element={<IntelPage />} />
        <Route path="/mobilization" element={<MobilizationPage />} />
        <Route path="/pets" element={<PetsPage />} />
        <Route path="/shop" element={<ShopPage />} />
        <Route path="/training" element={<TrainingPage />} />
        <Route path="/polar-terror" element={<PolarTerrorPage />} />
        <Route path="/bear-trap" element={<BearTrapPage />} />
        <Route path="/chief-order" element={<ChiefOrderPage />} />
        <Route path="/experts" element={<ExpertsPage />} />
        <Route path="/emulator" element={<EmulatorPage />} />
        <Route path="*" element={<Navigate to="/logs" replace />} />
      </Route>
    </Routes>
  </BrowserRouter>
);

export default App;
