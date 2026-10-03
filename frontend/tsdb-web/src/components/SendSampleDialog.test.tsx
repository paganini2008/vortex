import { describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { SendSampleDialog } from "./SendSampleDialog";

const known = [
  { dataType: "double" as const, category: "car", dimension: "fuel" },
  { dataType: "long" as const, category: "car", dimension: "speed" },
];

function ok(value: unknown) {
  return vi.spyOn(globalThis, "fetch").mockResolvedValue(new Response(JSON.stringify({ code: 1, msg: "ok", data: { value } })));
}

describe("SendSampleDialog", () => {
  it("asks for what is missing", async () => {
    render(<SendSampleDialog category="car" known={known} onClose={() => {}} onSent={() => {}} />);
    await userEvent.click(screen.getByRole("button", { name: "Send" }));
    expect(screen.getByRole("status")).toHaveTextContent("Enter a value.");
    await userEvent.type(screen.getByPlaceholderText("44"), "3");
    await userEvent.click(screen.getByRole("button", { name: "Send" }));
    expect(screen.getByRole("status")).toHaveTextContent("Enter a dimension.");
  });

  it("sends a value and takes an existing dimension's type", async () => {
    const f = ok("0.5");
    const onSent = vi.fn();
    render(<SendSampleDialog category="car" known={known} onClose={() => {}} onSent={onSent} />);
    const dimension = screen.getByPlaceholderText("speed");
    expect(dimension).toHaveFocus();
    await userEvent.type(dimension, "fuel");
    expect(screen.getByLabelText("Data type")).toHaveValue("double");
    await userEvent.type(screen.getByPlaceholderText("44"), "0.5");
    await userEvent.click(screen.getByRole("button", { name: "Send" }));
    expect(await screen.findByText("Sent 0.5 to fuel")).toBeInTheDocument();
    expect(f.mock.calls[0][0]).toBe("/tsd/push?t=double&c=car&d=fuel&v=0.5");
    expect(onSent).toHaveBeenCalled();
  });

  it("sends a random value for the chosen series", async () => {
    const f = ok(4321);
    render(<SendSampleDialog category="car" known={known} initial={known[1]} onClose={() => {}} onSent={() => {}} />);
    await userEvent.click(screen.getByRole("button", { name: "Send random" }));
    expect(await screen.findByText("Sent 4,321 to speed")).toBeInTheDocument();
    expect(f.mock.calls[0][0]).toBe("/tsd/test?t=long&c=car&d=speed");
  });

  it("shows why sending failed", async () => {
    vi.spyOn(globalThis, "fetch").mockResolvedValue(new Response(JSON.stringify({ code: 0, msg: "Not an integer: 1.5" }), { status: 400 }));
    render(<SendSampleDialog category="car" known={known} initial={known[1]} onClose={() => {}} onSent={() => {}} />);
    fireEvent.change(screen.getByLabelText("Data type"), { target: { value: "long" } });
    await userEvent.type(screen.getByPlaceholderText("44"), "1.5");
    await userEvent.click(screen.getByRole("button", { name: "Send" }));
    expect(await screen.findByText("Not an integer: 1.5")).toBeInTheDocument();
  });

  it("falls back to a generic message for unexpected errors", async () => {
    vi.spyOn(globalThis, "fetch").mockImplementation(() => {
      throw new Error("boom");
    });
    render(<SendSampleDialog category="car" known={known} initial={known[1]} onClose={() => {}} onSent={() => {}} />);
    await userEvent.click(screen.getByRole("button", { name: "Send random" }));
    expect(await screen.findByText("Can't reach the TSDB. Check that a node is running.")).toBeInTheDocument();
  });

  it("closes on Escape, the close button and the backdrop", async () => {
    const onClose = vi.fn();
    const { container } = render(<SendSampleDialog category="car" known={known} onClose={onClose} onSent={() => {}} />);
    await userEvent.keyboard("{Escape}");
    await userEvent.click(screen.getByRole("button", { name: "Close" }));
    fireEvent.mouseDown(container.firstElementChild!);
    expect(onClose).toHaveBeenCalledTimes(3);
  });
});
