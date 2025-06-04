import Link from "next/link";

export default function AboutPage() {
  return (
    <div className="min-h-screen bg-[rgb(213,208,202)] text-[rgb(21,21,21)] relative font-sans">
      {/* Subtle Dot Matrix Background */}
      <div className="absolute inset-0 bg-[radial-gradient(circle,rgb(110,110,110,0.3)_1.5px,transparent_1.5px)] bg-[size:14px_14px] opacity-50"></div>

      {/* Navbar */}
           {/* Navbar */}
           <nav className="flex justify-between items-center p-6 bg-[rgb(213,208,202)] relative z-10">
  <div className="text-2xl font-extrabold tracking-wide text-[rgb(21,21,21)]">blog.proj</div>
  <div className="space-x-6 text-sm font-medium text-[rgb(21,21,21)]">
    <a href="/" className="hover:text-[rgb(151,151,151)] transition">Home</a>
    <a href="/about" className="hover:text-[rgb(151,151,151)] transition">About</a>
    <a href="/create" className="hover:text-[rgb(151,151,151)] transition">Create Blog</a>
    <a href="/signup" className="hover:text-[rgb(151,151,151)] transition">Create Account</a>
    <a href="/signin" className="hover:text-[rgb(151,151,151)] transition">Sign In</a>
    <a href="/myprofile" className="hover:text-[rgb(151,151,151)] transition">My Profile</a>
  </div>
</nav>


      {/* About Section */}
      <section className="max-w-5xl mx-auto py-32 px-6 text-center">
        <h1 className="text-7xl font-bold">About Us</h1>
        <p className="mt-6 text-xl text-[rgba(21,21,21,0.6)] leading-relaxed">
          blog.proj is a minimalist blogging platform built for writers, thinkers, and creators. 
          Our mission is to provide a seamless, distraction-free space where you can share your ideas with the world. 
          Whether you're a storyteller, a journalist, or a casual blogger, blog.proj makes writing and reading effortless.
        </p>
      </section>
    </div>
  );
}
