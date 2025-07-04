"use client"

import type React from "react"

import { useEffect, useState } from "react"
import { useRouter } from "next/navigation"

interface ProtectedRouteProps {
  children: React.ReactNode
  allowedRoles?: string[]
}

export default function ProtectedRoute({ children, allowedRoles }: ProtectedRouteProps) {
  const [isAuthorized, setIsAuthorized] = useState(false)
  const [loading, setLoading] = useState(true)
  const router = useRouter()

  useEffect(() => {
 const token = localStorage.getItem("token")
  const role = localStorage.getItem("role")

  if (!token || !role) {
    router.push("/signin")
    return
  }

  if (allowedRoles && !allowedRoles.includes(role)) {
    router.push("/")
    return
  }

  setIsAuthorized(true)
  setLoading(false)
  }, [router, allowedRoles])

  if (loading) {
    return (
      <div className="flex justify-center overflow-auto items-center h-full">
        <div className="animate-spin rounded-full h-32 w-32 border-t-2 border-b-2 border-gray-900"></div>
      </div>
    )
  }

  return isAuthorized ? <>{children}</> : null
}
