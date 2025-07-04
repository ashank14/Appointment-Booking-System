"use client"

import type React from "react"
import { useState, useEffect } from "react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { useToast } from "@/hooks/use-toast"
import ProtectedRoute from "@/components/protected-route"
import { Calendar, Clock, User, Check, Trash2, Plus } from "lucide-react"
import api from "@/lib/api"

interface Appointment {
  appointmentId: number
  description: string
  slotId: string
  startTime: string
  endTime: string
  userUsername: string
  userEmail: string
  status: string
}

interface Slot {
  id: number
  description: string
  startTime: string
  endTime: string
  status: string
}

export default function ProviderDashboard() {
  const [appointments, setAppointments] = useState<Appointment[]>([])
  const [slots, setSlots] = useState<Slot[]>([])
  const [loading, setLoading] = useState(true)
  const [showCreateForm, setShowCreateForm] = useState(false)
  const [editingSlot, setEditingSlot] = useState<Slot | null>(null)
  const [newSlot, setNewSlot] = useState({
    description: "",
    startTime: "",
    endTime: "",
    status: "AVAILABLE",
  })
  const [appointmentSearch, setAppointmentSearch] = useState("");
  const [slotSearch, setSlotSearch] = useState("");
  const { toast } = useToast()

  useEffect(() => {
    fetchAppointments()
    fetchSlots()
  }, [])

  const fetchAppointments = async () => {
    try {
      const response = await api.get("/appointments/getProviderAppointments")
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
      const response = await api.get("/slots/getMySlots")
      setSlots(response.data)
    } catch (error: any) {
      toast({
        title: "Error",
        description: error.response?.data?.message || "Failed to fetch slots",
        variant: "destructive",
      })
    }
  }

  const createSlot = async (e: React.FormEvent) => {
    e.preventDefault()
    try {
      await api.post("/slots/addSlot", newSlot)
      toast({
        title: "Success",
        description: "Slot created successfully",
      })
      setNewSlot({
        description: "",
        startTime: "",
        endTime: "",
        status: "AVAILABLE",
      })
      setShowCreateForm(false)
      fetchSlots()
    } catch (error: any) {
      toast({
        title: "Error",
        description: error.response?.data?.message || "Failed to create slot",
        variant: "destructive",
      })
    }
  }

  const completeAppointment = async (id: number) => {
    try {
      await api.put(`/appointments/completeAppointment/${id}`)
      setAppointments(appointments.map((apt) => (apt.appointmentId === id ? { ...apt, status: "COMPLETED" } : apt)))
      toast({
        title: "Success",
        description: "Appointment marked as complete",
      })
    } catch (error: any) {
      toast({
        title: "Error",
        description: error.response?.data?.message || "Failed to complete appointment",
        variant: "destructive",
      })
    }
  }

  const deleteSlot = async (id: number) => {
    try {
      await api.delete(`/slots/${id}`)
      setSlots(slots.filter((slot) => slot.id !== id))
      toast({
        title: "Success",
        description: "Slot deleted successfully",
      })
    } catch (error: any) {
      toast({
        title: "Error",
        description: error.response?.data?.message || "Failed to delete slot",
        variant: "destructive",
      })
    }
  }

  const updateSlot = async () => {
    if (!editingSlot) return
    try {
      // Destructure and send only necessary fields
      const { description, startTime, endTime, status } = editingSlot
      await api.put(`/slots/updateSlot/${editingSlot.id}`, { description, startTime, endTime, status })

      toast({
        title: "Success",
        description: "Slot updated successfully",
      })
      setEditingSlot(null)
      fetchSlots()
      fetchAppointments()
    } catch (error: any) {
      toast({
        title: "Error",
        description: error.response?.data?.message || "Failed to update slot",
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
    <ProtectedRoute allowedRoles={["PROVIDER"]}>
      <div className="container mx-auto px-4 py-8">
        <h1 className="text-3xl font-bold mb-8">Provider Dashboard</h1>

        <div className="grid lg:grid-cols-2 gap-8">
          {/* Appointments */}
          <div>
            <h2 className="text-2xl font-semibold mb-4">My Appointments</h2>
            <Input
              placeholder="Search by Appointment ID"
              className="mb-4"
              value={appointmentSearch}
              onChange={e => setAppointmentSearch(e.target.value)}
              type="number"
            />
            <div className="space-y-4 max-h-[480px] overflow-y-auto pr-2">
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
                          Appointment ID: {appointment.appointmentId}
                        </CardTitle>
                        <Badge
                          variant={
                            appointment.status === "COMPLETED"
                              ? "default"
                              : appointment.status === "CANCELLED"
                              ? "destructive"
                              : "secondary"
                          }
                        >
                          {appointment.status}
                        </Badge>
                      </div>
                    </CardHeader>

                    <CardContent>
                      <CardTitle className="text-lg">{appointment.description}</CardTitle>

                      {/* Slot ID */}
                      <div className="text-sm text-gray-600 mb-2">
                        Slot ID: {appointment.slotId}
                      </div>

                      <div className="space-y-2 text-sm mb-4">
                        <div className="flex items-center gap-2">
                          <Calendar className="h-4 w-4" />
                          <span>{new Date(appointment.startTime).toLocaleDateString()}</span>
                        </div>
                        <div className="flex items-center gap-2">
                          <Clock className="h-4 w-4" />
                          <span>
                            {new Date(appointment.startTime).toLocaleTimeString()} -{" "}
                            {new Date(appointment.endTime).toLocaleTimeString()}
                          </span>
                        </div>
                        <div className="flex items-center gap-2">
                          <User className="h-4 w-4" />
                          <span>
                            {appointment.userUsername} ({appointment.userEmail})
                          </span>
                        </div>
                      </div>

                      {appointment.status !== "COMPLETED" && (
                        <Button
                          size="sm"
                          onClick={() => completeAppointment(appointment.appointmentId)}
                        >
                          <Check className="h-4 w-4 mr-2" />
                          Mark Complete
                        </Button>
                      )}
                    </CardContent>
                  </Card>
                ))}

              {appointments.length === 0 && (
                <Card>
                  <CardContent className="text-center py-8">
                    <p className="text-gray-500">No appointments found</p>
                  </CardContent>
                </Card>
              )}
            </div>
          </div>

          {/* Slots */}
          <div>
            <div className="flex justify-between items-center mb-4">
              <h2 className="text-2xl font-semibold">My Slots</h2>
              <Button onClick={() => setShowCreateForm(!showCreateForm)}>
                <Plus className="h-4 w-4 mr-2" />
                Create Slot
              </Button>
            </div>
            <Input
              placeholder="Search by Slot ID"
              className="mb-4"
              value={slotSearch}
              onChange={e => setSlotSearch(e.target.value)}
              type="number"
            />
            {showCreateForm && (
              <Card className="mb-6">
                <CardHeader>
                  <CardTitle>Create New Slot</CardTitle>
                </CardHeader>
                <CardContent>
                  <form onSubmit={createSlot} className="space-y-4">
                    <div>
                      <Label>Description</Label>
                      <Textarea
                        value={newSlot.description}
                        onChange={(e) => setNewSlot({ ...newSlot, description: e.target.value })}
                        required
                      />
                    </div>
                    <div>
                      <Label>Start Time</Label>
                      <Input
                        type="datetime-local"
                        value={newSlot.startTime}
                        onChange={(e) => setNewSlot({ ...newSlot, startTime: e.target.value })}
                        required
                      />
                    </div>
                    <div>
                      <Label>End Time</Label>
                      <Input
                        type="datetime-local"
                        value={newSlot.endTime}
                        onChange={(e) => setNewSlot({ ...newSlot, endTime: e.target.value })}
                        required
                      />
                    </div>
                    <div>
                      <Label>Status</Label>
                      <Select onValueChange={(value) => setNewSlot({ ...newSlot, status: value })}>
                        <SelectTrigger>
                          <SelectValue placeholder="Select status" />
                        </SelectTrigger>
                        <SelectContent>
                          <SelectItem value="AVAILABLE">Available</SelectItem>
                          <SelectItem value="UNAVAILABLE">Unavailable</SelectItem>
                        </SelectContent>
                      </Select>
                    </div>
                    <Button type="submit">Create Slot</Button>
                  </form>
                </CardContent>
              </Card>
            )}

            {/* Slot List */}
            <div className="space-y-4 max-h-[480px] overflow-y-auto pr-2">
              {slots
                .filter(slot =>
                  slotSearch === "" ||
                  slot.id.toString().includes(slotSearch)
                )
                .map((slot) => (
                <Card key={slot.id}>
                  <CardHeader>
                    <div className="flex justify-between items-start">
                      <CardTitle className="text-lg">Slot Id: {slot.id} </CardTitle>
                      <Badge variant={slot.status === "AVAILABLE" ? "default" : "secondary"}>{slot.status}</Badge>
                    </div>
                  </CardHeader>
                  <CardContent>
                    <div className="space-y-2 text-sm mb-4">
                      <CardTitle className="text-lg">{slot.description}</CardTitle>
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
                    </div>
                    <div className="flex gap-2">
                      <Button size="sm" onClick={() => setEditingSlot(slot)}>
                        Reschedule
                      </Button>
                      <Button variant="destructive" size="sm" onClick={() => deleteSlot(slot.id)}>
                        <Trash2 className="h-4 w-4 mr-2" />
                        Delete
                      </Button>
                    </div>
                  </CardContent>
                </Card>
              ))}
            </div>

            {/* Edit Slot */}
            {editingSlot && (
                <Card className="my-6">
                  <CardHeader>
                    <CardTitle>Update Slot</CardTitle>
                  </CardHeader>
                  <CardContent>
                    <form
                      onSubmit={(e) => {
                        e.preventDefault()
                        updateSlot()
                      }}
                      className="space-y-4"
                    >
                      <div>
                        <Label>Description</Label>
                        <Textarea
                          value={editingSlot.description}
                          onChange={(e) => setEditingSlot({ ...editingSlot, description: e.target.value })}
                          required
                        />
                      </div>
                      <div>
                        <Label>Start Time</Label>
                        <Input
                          type="datetime-local"
                          value={editingSlot.startTime}
                          onChange={(e) => setEditingSlot({ ...editingSlot, startTime: e.target.value })}
                          required
                        />
                      </div>
                      <div>
                        <Label>End Time</Label>
                        <Input
                          type="datetime-local"
                          value={editingSlot.endTime}
                          onChange={(e) => setEditingSlot({ ...editingSlot, endTime: e.target.value })}
                          required
                        />
                      </div>

                      {/* Remove status selector here */}

                      <div className="flex gap-2">
                        <Button type="submit">Update Slot</Button>
                        <Button type="button" variant="outline" onClick={() => setEditingSlot(null)}>
                          Cancel
                        </Button>
                      </div>
                    </form>
                  </CardContent>
                </Card>
              )}
          </div>
        </div>
      </div>
    </ProtectedRoute>
  )
}
