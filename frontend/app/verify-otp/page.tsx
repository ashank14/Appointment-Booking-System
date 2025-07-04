"use client"

import { useSearchParams, useRouter } from "next/navigation"
import { useState } from "react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { useToast } from "@/hooks/use-toast"
import api from "@/lib/api"

export default function VerifyOtpPage() {
  const [otp, setOtp] = useState("")
  const [loading, setLoading] = useState(false)
  const { toast } = useToast()
  const searchParams = useSearchParams()
  const username = searchParams.get("username") || ""
  const router = useRouter()

  const handleVerify = async (e: React.FormEvent) => {
    e.preventDefault()
    setLoading(true)

    try {
      await api.post(`/user/verify?username=${username}&otp=${otp}`)

      toast({
        title: "Account created!",
        description: "You can now sign in with your credentials.",
      })
      router.push("/signin")
    } catch (error: any) {
      toast({
        title: "Verification failed",
        description: error.response?.data?.message || "Invalid OTP",
        variant: "destructive",
      })
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="container mx-auto px-4 py-16">
      <div className="max-w-md mx-auto">
        <Card>
          <CardHeader>
            <CardTitle>Verify OTP</CardTitle>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleVerify} className="space-y-4">
              <div>
                <Label htmlFor="otp">Enter the OTP sent for {username}</Label>
                <Input
                  id="otp"
                  type="text"
                  value={otp}
                  onChange={(e) => setOtp(e.target.value)}
                  required
                />
              </div>

              <Button type="submit" className="w-full" disabled={loading}>
                {loading ? "Verifying..." : "Verify & Activate"}
              </Button>
            </form>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
