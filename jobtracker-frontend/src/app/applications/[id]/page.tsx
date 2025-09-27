// app/applications/[id]/page.tsx
import React from "react";
import { cookies } from "next/headers"; // to read cookie on server

interface Application {
  id: string;
  applicantName: string;
  approvedAmount: number;
  status: string;
  securityType: string;
  approvalDate: string;
  loanOfficerName?: string;
  notes?: string;
}

interface PageProps {
  params: Promise<{ id: string }>;
}

async function getApplication(id: string): Promise<Application> {
  const cookieStore = cookies();
  const token = (await cookieStore).get("token")?.value; // read JWT

  const res = await fetch(
    `${process.env.NEXT_PUBLIC_API_URL}/applications/${id}`,
    {
      cache: "no-store",
      headers: {
        Authorization: token ? `Bearer ${token}` : "",
        "Content-Type": "application/json",
      },
    }
  );

  if (!res.ok) {
    throw new Error(`Failed to fetch application: ${res.status}`);
  }

  return res.json();
}

export default async function ApplicationPage({ params }: PageProps) {
  const { id } = await params;
  const app = await getApplication(id);

  return (
    <div style={{ padding: "2rem" }}>
      <h1>Application Detail</h1>
      <ul>
        <li><strong>ID:</strong> {app.id}</li>
        <li><strong>Applicant Name:</strong> {app.applicantName}</li>
        <li><strong>Approved Amount:</strong> ${app.approvedAmount}</li>
        <li><strong>Status:</strong> {app.status}</li>
        <li><strong>Security Type:</strong> {app.securityType}</li>
        <li><strong>Approval Date:</strong> {new Date(app.approvalDate).toLocaleDateString()}</li>
        {app.loanOfficerName && <li><strong>Loan Officer:</strong> {app.loanOfficerName}</li>}
        {app.notes && <li><strong>Notes:</strong> {app.notes}</li>}
      </ul>
    </div>
  );
}
