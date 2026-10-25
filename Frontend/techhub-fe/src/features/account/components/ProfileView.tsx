"use client";

import { useState } from "react";
import Image from "next/image";
import {
  Camera,
  Save,
  ShieldCheck,
  Mail,
  Loader2,
  CheckCircle2,
  Upload,
} from "lucide-react";
import { mockUserProfile, type UserProfileDTO } from "@/mocks";

export function ProfileView() {
  const [profile, setProfile] = useState<UserProfileDTO>(mockUserProfile);
  const [firstName, setFirstName] = useState(profile.firstName);
  const [lastName, setLastName] = useState(profile.lastName);
  const [phone, setPhone] = useState(profile.phone);
  const [avatarPreview, setAvatarPreview] = useState(profile.avatarUrl);

  const [isSaving, setIsSaving] = useState(false);
  const [showToast, setShowToast] = useState(false);

  // Handle avatar upload simulation
  const handleAvatarChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      const tempUrl = URL.createObjectURL(file);
      setAvatarPreview(tempUrl);
    }
  };

  const handleSave = (e: React.FormEvent) => {
    e.preventDefault();
    setIsSaving(true);

    setTimeout(() => {
      setProfile((prev) => ({
        ...prev,
        firstName,
        lastName,
        phone,
        avatarUrl: avatarPreview,
      }));
      setIsSaving(false);
      setShowToast(true);
      setTimeout(() => setShowToast(false), 3000);
    }, 600);
  };

  const handleCancel = () => {
    setFirstName(profile.firstName);
    setLastName(profile.lastName);
    setPhone(profile.phone);
    setAvatarPreview(profile.avatarUrl);
  };

  return (
    <main className="w-full h-full flex flex-col gap-6">
      {/* Main Profile Form Card */}
      <form
        onSubmit={handleSave}
        className="bg-white rounded-2xl p-6 sm:p-8 shadow-sm border border-[#c3c5d9]/25 flex flex-col gap-6 flex-1 h-full"
      >
        {/* Header Title & Description */}
        <div className="flex flex-col gap-1 border-b border-[#eff4ff] pb-4">
          <h1 className="font-heading text-xl font-bold text-[#0b1c30]">
            Personal Profile &amp; Contact Credentials
          </h1>
          <p className="text-xs text-[#737688]">
            Manage your personal profile details, verified phone, and primary email credentials.
          </p>
        </div>

        {/* 2-Column Responsive Body: Left Form Inputs + Right Avatar Module */}
        <div className="flex flex-col-reverse md:flex-row items-stretch gap-8 lg:gap-10 flex-1">
          {/* Left Column: Form Fields (~65% width) */}
          <div className="flex-1 w-full flex flex-col gap-4">
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              {/* First Name */}
              <div className="flex flex-col gap-1.5">
                <label
                  htmlFor="first-name"
                  className="text-xs font-semibold text-[#0b1c30]"
                >
                  First Name
                </label>
                <input
                  id="first-name"
                  type="text"
                  value={firstName}
                  onChange={(e) => setFirstName(e.target.value)}
                  placeholder="First name"
                  required
                  className="w-full h-11 px-4 rounded-xl bg-[#eff4ff]/60 text-[#0b1c30] text-sm focus:bg-white focus:ring-2 focus:ring-[#0045cc]/20 outline-none transition-all placeholder:text-[#737688]/60 border border-[#c3c5d9]/20"
                />
              </div>

              {/* Last Name */}
              <div className="flex flex-col gap-1.5">
                <label
                  htmlFor="last-name"
                  className="text-xs font-semibold text-[#0b1c30]"
                >
                  Last Name
                </label>
                <input
                  id="last-name"
                  type="text"
                  value={lastName}
                  onChange={(e) => setLastName(e.target.value)}
                  placeholder="Last name"
                  required
                  className="w-full h-11 px-4 rounded-xl bg-[#eff4ff]/60 text-[#0b1c30] text-sm focus:bg-white focus:ring-2 focus:ring-[#0045cc]/20 outline-none transition-all placeholder:text-[#737688]/60 border border-[#c3c5d9]/20"
                />
              </div>
            </div>

            {/* Phone Number */}
            <div className="flex flex-col gap-1.5">
              <label
                htmlFor="phone-number"
                className="text-xs font-semibold text-[#0b1c30]"
              >
                Phone Number
              </label>
              <input
                id="phone-number"
                type="tel"
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                placeholder="(+84) 000 000 000"
                required
                className="w-full h-11 px-4 rounded-xl bg-[#eff4ff]/60 text-[#0b1c30] text-sm focus:bg-white focus:ring-2 focus:ring-[#0045cc]/20 outline-none transition-all placeholder:text-[#737688]/60 border border-[#c3c5d9]/20"
              />
            </div>

            {/* Registered Email (Read-Only) */}
            <div className="flex flex-col gap-1.5">
              <div className="flex items-center justify-between">
                <label
                  htmlFor="email-address"
                  className="text-xs font-semibold text-[#0b1c30]"
                >
                  Registered Email
                </label>
                {profile.emailVerified && (
                  <span className="text-[11px] font-semibold text-emerald-600 flex items-center gap-1">
                    <ShieldCheck className="w-3.5 h-3.5" />
                    Verified
                  </span>
                )}
              </div>
              <div className="relative flex items-center">
                <input
                  id="email-address"
                  type="email"
                  value={profile.email}
                  disabled
                  className="w-full h-11 pl-4 pr-10 rounded-xl bg-neutral-100 text-[#737688] text-sm outline-none cursor-not-allowed select-none border border-[#c3c5d9]/30"
                />
                <Mail className="absolute right-3.5 w-4 h-4 text-[#737688]" />
              </div>
            </div>

            {/* Action Buttons: Cancel & Save Changes */}
            <div className="pt-4 flex items-center gap-3 mt-auto">
              <button
                type="button"
                onClick={handleCancel}
                className="px-5 py-2.5 rounded-xl bg-[#eff4ff] hover:bg-[#dce9ff] text-[#0b1c30] text-xs font-semibold transition-colors cursor-pointer"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={isSaving}
                className="px-6 py-2.5 rounded-xl bg-[#0045cc] hover:bg-[#0d5bff] text-white text-xs font-semibold transition-all shadow-sm flex items-center justify-center gap-2 active:scale-[0.99] disabled:opacity-70 cursor-pointer"
              >
                {isSaving ? (
                  <>
                    <Loader2 className="w-4 h-4 animate-spin" />
                    <span>Saving...</span>
                  </>
                ) : (
                  <>
                    <Save className="w-4 h-4" />
                    <span>Save Changes</span>
                  </>
                )}
              </button>
            </div>
          </div>

          {/* Right Column: Profile Avatar Module (~35% width, stretches full height with left border) */}
          <div className="w-full md:w-64 flex flex-col items-center justify-center text-center p-6 bg-[#eff4ff]/40 md:bg-transparent rounded-2xl md:rounded-none md:border-l md:border-[#eff4ff] shrink-0 gap-5 self-stretch">
            {/* Avatar Circle with Camera Overlay */}
            <div className="relative group cursor-pointer">
              <div className="w-32 h-32 rounded-full overflow-hidden ring-4 ring-[#0045cc]/15 shadow-sm relative">
                <Image
                  src={avatarPreview}
                  alt={`${firstName} ${lastName}`}
                  fill
                  className="object-cover"
                  unoptimized
                />
              </div>
              <label
                htmlFor="avatar-input"
                className="absolute inset-0 bg-black/40 rounded-full flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity cursor-pointer"
              >
                <Camera className="w-7 h-7 text-white" />
              </label>
            </div>

            {/* Select Image Button */}
            <div className="flex flex-col items-center gap-2.5 w-full max-w-[200px]">
              <label
                htmlFor="avatar-input"
                className="cursor-pointer w-full py-2.5 rounded-xl bg-white hover:bg-[#eff4ff] text-[#0b1c30] hover:text-[#0045cc] text-xs font-semibold transition-all flex items-center justify-center gap-2 shadow-xs border border-[#c3c5d9]/40 active:scale-[0.99]"
              >
                <Upload className="w-3.5 h-3.5 text-[#0045cc]" />
                <span>Select Image</span>
                <input
                  id="avatar-input"
                  type="file"
                  accept="image/*"
                  onChange={handleAvatarChange}
                  className="hidden"
                />
              </label>

              {/* Requirements text */}
              <div className="text-[11px] text-[#737688] leading-relaxed flex flex-col gap-0.5">
                <span>File size: maximum 1 MB</span>
                <span>File extension: .JPEG, .PNG, .WEBP</span>
              </div>
            </div>
          </div>
        </div>
      </form>

      {/* Floating Save Toast Notification */}
      <div
        className={`fixed bottom-8 right-8 z-50 bg-[#0b1c30] text-white px-5 py-3 rounded-2xl shadow-xl flex items-center gap-3 transform transition-all duration-300 ${
          showToast
            ? "translate-y-0 opacity-100 pointer-events-auto"
            : "translate-y-12 opacity-0 pointer-events-none"
        }`}
      >
        <CheckCircle2 className="w-5 h-5 text-emerald-400 shrink-0" />
        <div className="flex flex-col text-left">
          <span className="text-xs font-bold">Profile Credentials Saved</span>
          <span className="text-[11px] text-neutral-300">
            Account information updated across TechHub nodes.
          </span>
        </div>
      </div>
    </main>
  );
}
