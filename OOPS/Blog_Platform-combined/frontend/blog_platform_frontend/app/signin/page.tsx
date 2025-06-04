"use client";

import React, { useState } from "react";
import Link from "next/link";

const SignInPage = () => {
  const [formData, setFormData] = useState({
    email: "",
    password: "",
  });

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    console.log("Form submitted:", formData);
    // Later, you will connect this to your Spring Boot backend
  };

  return (
    <div className="min-h-screen bg-[rgb(213,208,202)] text-[rgb(21,21,21)] relative font-sans">
      {/* Denser Dot Matrix Background */}
      <div className="absolute inset-0 bg-[radial-gradient(circle,rgb(110,110,110,0.2)_1px,transparent_1px)] bg-[size:8px_8px] opacity-50"></div>

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

      {/* Sign In Form */}
      <div className="p-8 w-full max-w-lg mx-auto mt-20 relative z-10">
        <h1 className="text-3xl font-bold text-center mb-6">Sign In</h1>
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-[rgb(21,21,21)]">Email</label>
            <input
              type="email"
              name="email"
              value={formData.email}
              onChange={handleChange}
              className="w-full p-2 border rounded-md bg-[rgb(213,208,202)] focus:outline-none focus:ring-2 focus:ring-[rgb(151,151,151)]"
              required
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-[rgb(21,21,21)]">Password</label>
            <input
              type="password"
              name="password"
              value={formData.password}
              onChange={handleChange}
              className="w-full p-2 border rounded-md bg-[rgb(213,208,202)] focus:outline-none focus:ring-2 focus:ring-[rgb(151,151,151)]"
              required
            />
          </div>
          <button
            type="submit"
            className="w-full py-2 bg-[rgb(21,21,21)] text-white rounded-lg hover:bg-[rgb(51,51,51)] transition font-semibold">
            Sign In
          </button>
        </form>
        <p className="mt-4 text-sm text-center">
          Don't have an account? <Link href="/signup" className="text-[rgb(21,21,21)] hover:text-[rgb(151,151,151)]">Sign Up</Link>
        </p>
      </div>
    </div>
  );
};

export default SignInPage;
