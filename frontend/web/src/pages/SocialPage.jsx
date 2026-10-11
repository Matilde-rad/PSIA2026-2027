import SocialPreview from '../previews/SocialPreview.jsx'

export default function SocialPage() {
  return (
    <>
      <div className="page-header">
        <h1 className="page-title">Social</h1>
        <p className="page-subtitle">Find people who like the same movies as you.</p>
      </div>
      <SocialPreview />
    </>
  )
}
