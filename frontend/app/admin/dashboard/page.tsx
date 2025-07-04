"use client"

import { useState, useEffect } from "react"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Button } from "@/components/ui/button"
import { Label } from "@/components/ui/label"
import { useToast } from "@/hooks/use-toast"
import { Badge } from "@/components/ui/badge"
import ProtectedRoute from "@/components/protected-route"
import { BarChart3, TrendingUp, Users, Calendar } from "lucide-react"
import api from "@/lib/api"

interface AnalyticsData {
  totalAppointments?: number
  cancellations?: number
  peakHours?: string[]
  cancellationRate?: number
}

interface SystemHealth{
    overallStatus?:String
    dbStatus?:String
    apiStatus?:String
}

export default function AdminDashboard() {
  const [providerId, setProviderId] = useState("")
  const [analyticsData, setAnalyticsData] = useState<AnalyticsData>({})
  const [loading, setLoading] = useState(false)
  const { toast } = useToast()
  const [providerAppointments, setProviderAppointments] = useState<any[]>([])
  const[systemHealth,setSystemHealth]=useState<SystemHealth>({})


  const fetchAppointmentCount = async () => {
    if (!providerId) return

    setLoading(true)
    try {
      const response = await api.get(`/admin/appointments/count/${providerId}`)
      setAnalyticsData((prev) => ({ ...prev, totalAppointments: response.data }))
    } catch (error: any) {
      toast({
        title: "Error",
        description: error.response?.data?.message || "Failed to fetch appointment count",
        variant: "destructive",
      })
    } finally {
      setLoading(false)
    }
  }

  const fetchCancellations = async () => {
    if (!providerId) return

    setLoading(true)
    try {
      const response = await api.get(`/admin/cancellations/count/${providerId}`)
      setAnalyticsData((prev) => ({ ...prev, cancellations: response.data }))
    } catch (error: any) {
      toast({
        title: "Error",
        description: error.response?.data?.message || "Failed to fetch cancellations",
        variant: "destructive",
      })
    } finally {
      setLoading(false)
    }
  }

    const fetchProviderAppointments = async () => {
    if (!providerId) return

    setLoading(true)
    try {
      const response = await api.get(`/admin/appointments/getAppointmentList/${providerId}`)
      setProviderAppointments(response.data)
      toast({
        title: "Success",
        description: `Found ${response.data.length} appointments for provider`,
      })
    } catch (error: any) {
      toast({
        title: "Error",
        description: error.response?.data?.message || "Failed to fetch provider appointments",
        variant: "destructive",
      })
    } finally {
      setLoading(false)
    }
  }


  const fetchPeakHours = async () => {
    setLoading(true)
    try {
      const response = await api.get("/admin/appointments/peak-hours")
      setAnalyticsData((prev) => ({ ...prev, peakHours: response.data }))
    } catch (error: any) {
      toast({
        title: "Error",
        description: error.response?.data?.message || "Failed to fetch peak hours",
        variant: "destructive",
      })
    } finally {
      setLoading(false)
    }
  }

  const fetchCancellationRate = async () => {
    if (!providerId) return

    setLoading(true)
    try {
      const response = await api.get(`/admin/cancellations/rate/${providerId}`)
      setAnalyticsData((prev) => ({ ...prev, cancellationRate: response.data }))
    } catch (error: any) {
      toast({
        title: "Error",
        description: error.response?.data?.message || "Failed to fetch cancellation rate",
        variant: "destructive",
      })
    } finally {
      setLoading(false)
    }
  }

  const fetchSystemHealth =async () =>{
      try{

          const response= await api.get(`/actuator/health`)
          setSystemHealth({
              overallStatus:response.data.status,
              dbStatus:response.data.components.db.status,
              apiStatus:response.data.components.ping.status

          })

      } catch (error: any) {
             toast({
               title: "Error",
               description: error.response?.data?.message || "Failed to fetch cancellation rate",
               variant: "destructive",
             })
           } finally {
             setLoading(false)
           }
  }

  useEffect(() => {
    fetchPeakHours()
    fetchSystemHealth()
  }, [])

  return (
    <ProtectedRoute allowedRoles={["ADMIN"]}>
      <div className="container w-full h-full overflow-hidden mx-auto px-4 py-8">
        <h1 className="text-3xl font-bold mb-8">Admin Dashboard</h1>

        {/* Provider Analytics Controls */}
        <Card className="mb-8">
          <CardHeader>
            <CardTitle>Provider Analytics</CardTitle>
            <CardDescription>Enter a provider ID to view specific analytics</CardDescription>
          </CardHeader>
          <CardContent>
            <div className="flex gap-4 items-end">
              <div className="flex-1">
                <Label htmlFor="providerId">Provider ID</Label>
                <Input
                  id="providerId"
                  value={providerId}
                  onChange={(e) => setProviderId(e.target.value)}
                  placeholder="Enter provider ID"
                />
              </div>
              <div className="flex gap-2">
                <Button onClick={fetchAppointmentCount} disabled={loading || !providerId}>
                  Get Appointments
                </Button>
                <Button onClick={fetchCancellations} disabled={loading || !providerId}>
                  Get Cancellations
                </Button>
                <Button onClick={fetchCancellationRate} disabled={loading || !providerId}>
                  Get Rate
                </Button>
                <Button onClick={fetchProviderAppointments} disabled={loading || !providerId}>
                  Get All Appointments
                </Button>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Analytics Cards */}
        <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">Total Appointments</CardTitle>
              <Calendar className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{analyticsData.totalAppointments ?? "--"}</div>
              <p className="text-xs text-muted-foreground">For selected provider</p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">Cancellations</CardTitle>
              <TrendingUp className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{analyticsData.cancellations ?? "--"}</div>
              <p className="text-xs text-muted-foreground">For selected provider</p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">Cancellation Rate</CardTitle>
              <BarChart3 className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">
                {analyticsData.cancellationRate + "%" ? `${analyticsData.cancellationRate}%` : "--"}
              </div>
              <p className="text-xs text-muted-foreground">For selected provider</p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">System Status</CardTitle>
              <Users className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold text-green-600">Active</div>
              <p className="text-xs text-muted-foreground">All systems operational</p>
            </CardContent>
          </Card>
        </div>

        {/* Peak Hours */}
        <Card>
          <CardHeader>
            <CardTitle>Peak Booking Hours</CardTitle>
            <CardDescription>Hours with the highest booking activity across all providers</CardDescription>
          </CardHeader>
          <CardContent>
              {analyticsData.peakHours && (
                <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                  {Object.entries(analyticsData.peakHours).map(([hour, count], index) => (
                    <div key={index} className="text-center p-4 bg-gray-50 rounded-lg">
                      <div className="text-2xl font-bold text-blue-600">{hour}:00</div>
                      <div className="text-sm text-gray-600">{count} bookings</div>
                    </div>
                  ))}
                </div>
              )}  
          </CardContent>
        </Card>


        {/* Provider Appointments */}
        {providerAppointments.length > 0 && (
          <Card className="mt-8">
            <CardHeader>
              <CardTitle>Provider Appointments</CardTitle>
              <CardDescription>All appointments for Provider ID: {providerId}</CardDescription>
            </CardHeader>
            <CardContent>
              <div className="grid gap-4">
                {providerAppointments.map((appointment, index) => (
                  <div key={appointment.appointmentId || index} className="border rounded-lg p-4 bg-gray-50">
                    <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-4">
                      <div>
                        <div className="text-sm font-medium text-gray-600">Appointment ID</div>
                        <div className="text-sm">{appointment.appointmentId || "N/A"}</div>
                      </div>
                      <div>
                        <div className="text-sm font-medium text-gray-600">Description</div>
                        <div className="text-sm">{appointment.description || "N/A"}</div>
                      </div>
                      <div>
                        <div className="text-sm font-medium text-gray-600">User</div>
                        <div className="text-sm">{appointment.providerUsername || appointment.userName || "N/A"}</div>
                        <div className="text-xs text-gray-500">{appointment.providerEmail || "N/A"}</div>
                      </div>
                      <div>
                        <div className="text-sm font-medium text-gray-600">Status</div>
                        <Badge
                          variant={
                            appointment.status === "COMPLETED"
                              ? "default"
                              : appointment.status === "CONFIRMED"
                                ? "secondary"
                                : "outline"
                          }
                        >
                          {appointment.status || "UNKNOWN"}
                        </Badge>
                      </div>
                      <div>
                        <div className="text-sm font-medium text-gray-600">Start Time</div>
                        <div className="text-sm">
                          {appointment.startTime ? new Date(appointment.startTime).toLocaleString() : "N/A"}
                        </div>
                      </div>
                      <div>
                        <div className="text-sm font-medium text-gray-600">End Time</div>
                        <div className="text-sm">
                          {appointment.endTime ? new Date(appointment.endTime).toLocaleString() : "N/A"}
                        </div>
                      </div>
                      <div>
                        <div className="text-sm font-medium text-gray-600">Slot ID</div>
                        <div className="text-sm">{appointment.slotId || "N/A"}</div>
                      </div>
                      <div>
                      </div>
                    </div>
                  </div>
                ))}
              </div>

              {providerAppointments.length === 0 && (
                <div className="text-center py-8 text-gray-500">No appointments found for this provider</div>
              )}
            </CardContent>
          </Card>
        )}
        {/* System Overview */}
        <div className="mt-8">
          <Card>
            <CardHeader>
              <CardTitle>System Health</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                <div className="flex justify-between items-center">
                  <span>Overall Status</span>
                  <span className="text-green-600 font-semibold">{systemHealth.overallStatus}</span>
                </div>
                <div className="flex justify-between items-center">
                  <span>Database Status</span>
                  <span className="text-green-600 font-semibold">{systemHealth.dbStatus}</span>
                </div>
                <div className="flex justify-between items-center">
                  <span>API Status</span>
                  <span className="text-green-600 font-semibold">{systemHealth.apiStatus}</span>
                </div>
              </div>
            </CardContent>
          </Card>


        </div>
      </div>
    </ProtectedRoute>
  )
}
