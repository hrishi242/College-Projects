import React from "react";

const LandingPage = () => {
  return (
    <div className="min-h-screen bg-[rgb(213,208,202)] text-[rgb(21,21,21)] relative font-sans">
      {/* Subtle Dot Matrix Background */}
      <div className="absolute inset-0 bg-[radial-gradient(circle,rgb(110,110,110,0.2)_1.6px,transparent_1.6px)] bg-[size:14px_14px] opacity-50"></div>
      
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


      {/* Hero Section */}
      <header className="text-center py-32 relative z-10">
        <h1 className="text-7xl font-extrabold text-[rgb(21,21,21)] leading-tight">A Simple Yet Powerful Blogging Platform</h1>
        <p className="mt-6 text-xl text-[rgba(21,21,21,0.6)]">Share your thoughts, inspire others, and build your online presence with ease.</p>
        <button className="mt-8 px-8 py-4 bg-[rgb(21,21,21)] text-white rounded-lg hover:bg-[rgb(51,51,51)] hover:text-white transition font-semibold">Start Blogging Now</button>
      </header>

      {/* Blog Preview Section */}
      <section className="max-w-6xl mx-auto py-20 px-6 relative z-10 grid grid-cols-1 md:grid-cols-2 gap-12">
        {Array(4).fill(0).map((_, i) => (
          <div key={i} className="flex items-center space-x-6">
            <div className="bg-[rgb(151,151,151)] h-48 w-48 rounded-lg"></div>
            <div>
              <p className="text-[rgba(21,21,21,0.6)] text-sm">MAR, 2025</p>
              <h2 className="text-3xl font-bold text-[rgb(21,21,21)]">Blog Post Title {i + 1}</h2>
              <p className="mt-2 text-[rgba(21,21,21,0.6)]">A brief summary of the blog post goes here, offering an intriguing preview of the content.</p>
              <button className="mt-4 px-4 py-2 bg-[rgb(21,21,21)] text-white rounded-lg hover:bg-[rgb(51,51,51)] hover:text-white transition font-semibold">Read More</button>
            </div>
          </div>
        ))}
      </section>
    </div>
  );
};

export default LandingPage;
