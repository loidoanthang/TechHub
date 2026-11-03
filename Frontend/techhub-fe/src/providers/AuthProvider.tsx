"use client";

import React, { createContext, useContext, useEffect, useState, useCallback } from "react";
import { useRouter } from "next/navigation";
import { authApi } from "@/features/auth/api/authApi";
import {
  getAccessToken,
  getRefreshToken,
  setTokens,
  clearTokens,
} from "@/lib/api/token";
import { ROUTES, STORAGE_KEYS } from "@/lib/constants";
import { isSafeRelativeUrl } from "@/lib/security";
import type { UserProfile } from "@/types/user";
import type { AuthTokens } from "@/types/auth";

interface AuthContextType {
  user: UserProfile | null;
  isLoading: boolean;
  isAuthenticated: boolean;
  login: (tokens: AuthTokens, redirectUrl?: string) => Promise<void>;
  logout: () => Promise<void>;
  refreshProfile: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<UserProfile | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const router = useRouter();

  const refreshProfile = useCallback(async () => {
    try {
      const response = await authApi.getProfile();
      if (response && response.success && response.data) {
        setUser(response.data);
        if (typeof window !== "undefined") {
          localStorage.setItem(STORAGE_KEYS.USER, JSON.stringify(response.data));
        }
      } else {
        setUser(null);
      }
    } catch {
      setUser(null);
    }
  }, []);

  // Initialize Auth state from storage / token
  useEffect(() => {
    const initAuth = async () => {
      const accessToken = getAccessToken();
      const refreshToken = getRefreshToken();

      if (!accessToken && !refreshToken) {
        setUser(null);
        setIsLoading(false);
        return;
      }

      // Try reading cached user first to prevent UI flickering
      if (typeof window !== "undefined") {
        const cachedUser = localStorage.getItem(STORAGE_KEYS.USER);
        if (cachedUser) {
          try {
            setUser(JSON.parse(cachedUser));
          } catch {
            // ignore
          }
        }
      }

      await refreshProfile();
      setIsLoading(false);
    };

    initAuth();
  }, [refreshProfile]);

  const login = async (tokens: AuthTokens, redirectUrl?: string) => {
    setTokens(tokens.accessToken, tokens.refreshToken);
    await refreshProfile();
    // Prevent Open Redirect (CWE-601): Strictly validate relative path
    if (isSafeRelativeUrl(redirectUrl)) {
      router.push(redirectUrl as string);
    } else {
      router.push(ROUTES.HOME);
    }
  };

  const logout = async () => {
    try {
      const refreshToken = getRefreshToken();
      if (refreshToken) {
        await authApi.logout({ refreshToken });
      }
    } catch {
      // ignore
    } finally {
      clearTokens();
      setUser(null);
      router.push(ROUTES.LOGIN);
    }
  };

  const value = {
    user,
    isLoading,
    isAuthenticated: !!user,
    login,
    logout,
    refreshProfile,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
}
