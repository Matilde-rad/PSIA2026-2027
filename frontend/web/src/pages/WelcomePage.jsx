import { useNavigate } from 'react-router-dom'
import { useAuth } from '../auth.jsx'
import ProfileForm from '../components/ProfileForm.jsx'

// First screen after registering: the profile is what the recommendations are made from.
export default function WelcomePage() {
  const { user, finishOnboarding } = useAuth()
  const navigate = useNavigate()

  function done() {
    finishOnboarding()
    navigate('/', { replace: true })
  }

  return (
    <div className="auth-page">
      <div className="auth-card auth-card-wide">
        <span className="wordmark">
          cine<span>mate</span>
        </span>
        <h1 className="auth-title">Welcome, {user.username}</h1>
        <p className="auth-subtitle">
          Tell us what you like and we'll have recommendations ready for you. You can change all of
          this later on your profile.
        </p>
        <ProfileForm
          askDirectors={false}
          submitLabel="Save and continue"
          onSaved={done}
          secondary={
            <button className="btn btn-ghost" type="button" onClick={done}>
              Skip for now
            </button>
          }
        />
      </div>
    </div>
  )
}
