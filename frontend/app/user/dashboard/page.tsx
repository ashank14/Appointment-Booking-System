"use client"

import { useState, useEffect } from "react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { useToast } from "@/hooks/use-toast"
import ProtectedRoute from "@/components/protected-route"
import { Calendar, Clock, User, X } from "lucide-react"
import api from "@/lib/api"

interface Appointment {
  appointmentId: number
  description: string
  slotId: number
  startTime: string
  endTime: string
  providerUsername: string
  providerEmail: string
  status: string
}

interface Slot {
  id: number
  description: string
  startTime: string
  endTime: string
  status: string
  providerUsername: string
  providerEmail: string
  providerSpecialization: string
}

export default function UserDashboard() {
  const [appointments, setAppointments] = useState<Appointment[]>([])
  const [slots, setSlots] = useState<Slot[]>([])
  const [queueSizes, setQueueSizes] = useState<{ [key: number]: number }>({})
  const [searchId, setSearchId] = useState("")
  const [loading, setLoading] = useState(true)
  const [appointmentSearch, setAppointmentSearch] = useState("")
  const [slotSpecializationSearch, setSlotSpecializationSearch] = useState("")
  const { toast } = useToast()

  useEffect(() => {
    fetchAppointments()
    fetchSlots()
  }, [])

  const fetchAppointments = async () => {
    try {
      const response = await api.get("/appointments/getUserAppointments")
      setAppointments(response.data)
    } catch (error: any) {
      toast({
        title: "Error",
        description: error.response?.data?.message || "Failed to fetch appointments",
        variant: "destructive",
      })
    } finally {
      setLoading(false)
    }
  }

  const fetchSlots = async () => {
    try {
      const response = await api.get("/slots/getAllSlots")
      setSlots(response.data)
      const sizes = await Promise.all(
        response.data.map(async (slot: Slot) => {
          const res = await api.get(`/queue/${slot.id}`)
          return { id: slot.id, size: res.data }
        })
      )
      const sizeMap: { [key: number]: number } = {}
      sizes.forEach(({ id, size }) => {
        sizeMap[id] = size
      })
      setQueueSizes(sizeMap)
    } catch (error: any) {
      toast({
        title: "Error",
        description: error.response?.data?.message || "Failed to fetch slots",
        variant: "destructive",
      })
    }
  }

  const cancelAppointment = async (id: number) => {
    try {
      await api.delete(`/appointments/cancel/${id}`)
      setAppointments(appointments.filter((apt) => apt.appointmentId !== id))
      toast({ title: "Success", description: "Appointment cancelled successfully" })
    } catch (error: any) {
      toast({
        title: "Error",
        description: error.response?.data?.message || "Failed to cancel appointment",
        variant: "destructive",
      })
    }
  }

  const bookSlot = async (slotId: number) => {
    try {
      await api.post(`/appointments/createAppointment/${slotId}`)
      toast({ title: "Success", description: "Appointment booked successfully" })
      fetchAppointments()
      fetchSlots()
    } catch (error: any) {
      toast({
        title: "Error",
        description: error.response?.data?.message || "Failed to book appointment",
        variant: "destructive",
      })
    }
  }

  const joinQueue = async (slotId: number) => {
    try {
      await api.post(`/queue/join/${slotId}`)
      toast({ title: "Success", description: "Joined queue successfully" })
      fetchSlots()
    } catch (error: any) {
      toast({
        title: "Error",
        description: error.response?.data?.message || "Failed to join queue",
        variant: "destructive",
      })
    }
  }

  const leaveQueue = async (slotId: number) => {
    try {
      await api.post(`/queue/leave/${slotId}`)
      toast({ title: "Left Queue", description: "Left queue successfully" })
      fetchSlots()
    } catch (error: any) {
      toast({
        title: "Error",
        description: error.response?.data?.message || "Failed to leave queue",
        variant: "destructive",
      })
    }
  }

  if (loading) {
    return (
      <div className="flex justify-center items-center min-h-screen">
        <div className="animate-spin rounded-full h-32 w-32 border-t-2 border-b-2 border-gray-900"></div>
      </div>
    )
  }

  return (
    <ProtectedRoute allowedRoles={["USER"]}>
      <div className="container mx-auto px-4 py-8">
        <h1 className="text-3xl font-bold mb-8">User Dashboard</h1>

        <div className="grid lg:grid-cols-2 gap-8">
          {/* My Appointments */}
          <div>
            <h2 className="text-2xl font-semibold mb-4">My Appointments</h2>
            <Input
              placeholder="Search by Appointment ID"
              className="mb-4"
              value={appointmentSearch}
              onChange={e => setAppointmentSearch(e.target.value)}
              type="number"
            />
            <div className="space-y-4 max-h-[550px] overflow-y-auto pr-2">
              {appointments
                .filter(appointment =>
                  appointmentSearch === "" ||
                  appointment.appointmentId.toString().includes(appointmentSearch)
                )
                .map((appointment) => (
                  <Card key={appointment.appointmentId}>
                    <CardHeader>
                      <div className="flex justify-between items-start">
                        <CardTitle className="text-lg">
                          Apt #{appointment.appointmentId} | Slot #{appointment.slotId}
                        </CardTitle>
                        <Badge variant={appointment.status === "CONFIRMED" ? "default" : "secondary"}>
                          {appointment.status}
                        </Badge>
                      </div>
                    </CardHeader>
                    <CardContent>
                      <div className="space-y-2 text-sm">
                        <div className="flex items-center gap-2">
                          <Calendar className="h-4 w-4" />
                          <span>{new Date(appointment.startTime).toLocaleDateString()}</span>
                        </div>
                        <div className="flex items-center gap-2">
                          <Clock className="h-4 w-4" />
                          <span>
                            {new Date(appointment.startTime).toLocaleTimeString()} - {new Date(appointment.endTime).toLocaleTimeString()}
                          </span>
                        </div>
                        <div className="flex items-center gap-2">
                          <User className="h-4 w-4" />
                          <span>
                            {appointment.providerUsername} ({appointment.providerEmail})
                          </span>
                        </div>
                      </div>
                      <Button
                        variant="destructive"
                        size="sm"
                        className="mt-4"
                        onClick={() => cancelAppointment(appointment.appointmentId)}
                      >
                        <X className="h-4 w-4 mr-2" /> Cancel
                      </Button>
                    </CardContent>
                  </Card>
                ))}
            </div>
          </div>

          {/* Available Slots */}
          <div>
            <h2 className="text-2xl font-semibold mb-4">Available Slots</h2>
            <Input
              placeholder="Search by Specialization"
              className="mb-4"
              value={slotSpecializationSearch}
              onChange={e => setSlotSpecializationSearch(e.target.value)}
            />
            <div className="space-y-4 max-h-[550px] overflow-y-auto pr-2">
              {slots
                .filter(slot =>
                  slotSpecializationSearch === "" ||
                  (slot.providerUsername && slot.providerUsername.toLowerCase().includes(slotSpecializationSearch.toLowerCase())) ||
                  (slot.providerEmail && slot.providerEmail.toLowerCase().includes(slotSpecializationSearch.toLowerCase())) ||
                  (slot.providerSpecialization && slot.providerSpecialization.toLowerCase().includes(slotSpecializationSearch.toLowerCase()))
                )
                .filter(slot => slot.status != "EXPIRED")
                .map((slot) => (
                  <Card key={slot.id}>
                    <CardHeader>
                      <div className="flex justify-between items-start">
                        <CardTitle className="text-lg">
                          #{slot.id} {slot.description}
                        </CardTitle>
                        <Badge variant="outline">{slot.status}</Badge>
                      </div>
                    </CardHeader>
                    <CardContent>
                      <div className="space-y-2 text-sm">
                        <div className="flex items-center gap-2">
                          <Calendar className="h-4 w-4" />
                          <span>{new Date(slot.startTime).toLocaleDateString()}</span>
                        </div>
                        <div className="flex items-center gap-2">
                          <Clock className="h-4 w-4" />
                          <span>
                            {new Date(slot.startTime).toLocaleTimeString()} - {new Date(slot.endTime).toLocaleTimeString()}
                          </span>
                        </div>
                        <div className="flex items-center gap-2">
                          <User className="h-4 w-4" />
                          <span>
                            {slot.providerUsername} ({slot.providerEmail})
                          </span>
                        </div>
                        <div className="flex items-center gap-2">
                          <span className="font-medium text-gray-500">Specialization:</span>
                          <span>{slot.providerSpecialization}</span>
                        </div>
                        <div className="text-sm">Queue size: {queueSizes[slot.id] ?? 0}</div>
                      </div>
                      <div className="flex gap-2 mt-4">
                        <Button className="flex-1" onClick={() => bookSlot(slot.id)}>
                          Book Appointment
                        </Button>
                        <Button className="flex-1" variant="secondary" onClick={() => joinQueue(slot.id)}>
                          Join Queue
                        </Button>
                        <Button className="flex-1" variant="outline" onClick={() => leaveQueue(slot.id)}>
                          Leave Queue
                        </Button>
                      </div>
                    </CardContent>
                  </Card>
                ))}
            </div>
          </div>
        </div>
      </div>
    </ProtectedRoute>
  )
}
