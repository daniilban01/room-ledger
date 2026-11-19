import { Navigate, Route, Routes } from 'react-router'
import { AccountPage } from './auth/AccountPage'
import { LoginPage } from './auth/LoginPage'
import { RegisterPage } from './auth/RegisterPage'
import { RequireAuth } from './auth/RequireAuth'
import { SettleUpPage } from './balances/SettleUpPage'
import { ExpenseFormPage } from './expenses/ExpenseFormPage'
import { GroupPage } from './groups/GroupPage'
import { GroupsPage } from './groups/GroupsPage'
import { AppLayout } from './ui/AppLayout'

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      {/* Nested layout routes: RequireAuth checks the login, AppLayout adds the bottom navigation. */}
      <Route element={<RequireAuth />}>
        <Route element={<AppLayout />}>
          <Route path="/" element={<GroupsPage />} />
          <Route path="/groups/:groupId" element={<GroupPage />} />
          <Route path="/groups/:groupId/settle" element={<SettleUpPage />} />
          <Route path="/groups/:groupId/expenses/new" element={<ExpenseFormPage />} />
          <Route path="/groups/:groupId/expenses/:expenseId" element={<ExpenseFormPage />} />
          <Route path="/account" element={<AccountPage />} />
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}
